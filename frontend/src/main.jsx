import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { api, refresh, setToken, signOut } from "./api";
import "./style.css";
const stateNames =
  "Alabama|AL,Alaska|AK,Arizona|AZ,Arkansas|AR,California|CA,Colorado|CO,Connecticut|CT,Delaware|DE,District of Columbia|DC,Florida|FL,Georgia|GA,Hawaii|HI,Idaho|ID,Illinois|IL,Indiana|IN,Iowa|IA,Kansas|KS,Kentucky|KY,Louisiana|LA,Maine|ME,Maryland|MD,Massachusetts|MA,Michigan|MI,Minnesota|MN,Mississippi|MS,Missouri|MO,Montana|MT,Nebraska|NE,Nevada|NV,New Hampshire|NH,New Jersey|NJ,New Mexico|NM,New York|NY,North Carolina|NC,North Dakota|ND,Ohio|OH,Oklahoma|OK,Oregon|OR,Pennsylvania|PA,Rhode Island|RI,South Carolina|SC,South Dakota|SD,Tennessee|TN,Texas|TX,Utah|UT,Vermont|VT,Virginia|VA,Washington|WA,West Virginia|WV,Wisconsin|WI,Wyoming|WY"
    .split(",")
    .map((x) => x.split("|"));
const emptyAddress = () => ({
    line1: "",
    line2: "",
    city: "",
    state: "KS",
    zip: "",
    country: "US",
  }),
  emptyContact = () => ({ firstName: "", lastName: "", email: "", phone: "" });
function Field({ label, ...props }) {
  return (
    <label className="field">
      {label}
      <input {...props} />
    </label>
  );
}
function Select({ label, children, ...props }) {
  return (
    <label className="field">
      {label}
      <select {...props}>{children}</select>
    </label>
  );
}
function AddressForm({ value, onChange, title }) {
  const set = (k, v) => onChange({ ...value, [k]: v });
  return (
    <fieldset>
      <legend>{title}</legend>
      <Field
        label="Street address"
        required
        maxLength={160}
        value={value.line1}
        onChange={(e) => set("line1", e.target.value)}
      />
      <Field
        label="Apartment or suite"
        maxLength={160}
        value={value.line2 || ""}
        onChange={(e) => set("line2", e.target.value)}
      />
      <div className="grid">
        <Field
          label="City"
          required
          value={value.city}
          onChange={(e) => set("city", e.target.value)}
        />
        <Select
          label="State"
          value={value.state}
          onChange={(e) => set("state", e.target.value)}
        >
          {stateNames.map(([name, code]) => (
            <option key={code} value={code}>
              {name} ({code})
            </option>
          ))}
        </Select>
        <Field
          label="ZIP code"
          required
          pattern="[0-9]{5}(-[0-9]{4})?"
          placeholder="66251 or 66251-1234"
          value={value.zip}
          onChange={(e) => set("zip", e.target.value)}
        />
      </div>
      <small>
        Shipping is available to the 50 U.S. states and Washington DC.
      </small>
    </fieldset>
  );
}
function ContactForm({ value, onChange, title }) {
  const set = (k, v) => onChange({ ...value, [k]: v });
  return (
    <fieldset>
      <legend>{title}</legend>
      <div className="grid">
        <Field
          label="First name"
          required
          maxLength={80}
          value={value.firstName}
          onChange={(e) => set("firstName", e.target.value)}
        />
        <Field
          label="Last name"
          required
          maxLength={80}
          value={value.lastName}
          onChange={(e) => set("lastName", e.target.value)}
        />
      </div>
      <div className="grid">
        <Field
          label="Email"
          required
          type="email"
          value={value.email}
          onChange={(e) => set("email", e.target.value)}
        />
        <Field
          label="Telephone"
          required
          pattern="[+0-9 ()\-]{7,40}"
          value={value.phone}
          onChange={(e) => set("phone", e.target.value)}
        />
      </div>
    </fieldset>
  );
}
const money = (n) =>
  new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(
    n,
  );
const contactValid = (c) =>
  c &&
  c.firstName.trim() &&
  c.lastName.trim() &&
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(c.email) &&
  /^[+0-9 ()-]{7,40}$/.test(c.phone);
const addressValid = (a) =>
  a &&
  a.line1.trim() &&
  a.city.trim() &&
  /^[0-9]{5}(-[0-9]{4})?$/.test(a.zip) &&
  a.country === "US";
const profileValid = (p) =>
  contactValid(p.primary) &&
  (!p.alternate || contactValid(p.alternate)) &&
  addressValid(p.mailing) &&
  addressValid(p.shipping) &&
  (p.kind !== "BUSINESS" || p.businessName?.trim());
export function App() {
  const [user, setUser] = useState(null),
    [page, setPage] = useState(
      new URLSearchParams(location.search).has("reset")
        ? "reset"
        : location.hash.slice(1) || "home",
    ),
    [previous, setPrevious] = useState("home"),
    [message, setMessage] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [ready, setReady] = useState(false);
  const [cart, setCart] = useState([]),
    [profile, setProfile] = useState(null),
    [shipping, setShipping] = useState(emptyAddress()),
    [quote, setQuote] = useState(null),
    [order, setOrder] = useState(null),
    [attempt, setAttempt] = useState(null);
  function navigate(next) {
    setPrevious(page);
    setPage(next);
    location.hash = next;
    setError("");
    setMessage("");
    window.scrollTo?.(0, 0);
  }
  async function run(action) {
    setBusy(true);
    setError("");
    try {
      return await action();
    } catch (e) {
      setError(
        e.message + (e.correlationId ? ` Reference: ${e.correlationId}` : ""),
      );
      return null;
    } finally {
      setBusy(false);
    }
  }
  async function loadCart() {
    const data = await api("/cart");
    setCart(data);
    return data;
  }
  useEffect(() => {
    refresh()
      .then((r) => setUser(r.user))
      .catch(() => {})
      .finally(() => setReady(true));
    const hash = () => setPage(location.hash.slice(1) || "home");
    window.addEventListener("hashchange", hash);
    return () => window.removeEventListener("hashchange", hash);
  }, []);
  useEffect(() => {
    if (user) run(loadCart);
    else setCart([]);
  }, [user]);
  async function requireUser() {
    if (!user) {
      navigate("login");
      return false;
    }
    return true;
  }
  async function toShipping() {
    await run(async () => {
      const p = await api("/me/profile");
      setProfile(p);
      setShipping(p.shipping);
      setQuote(null);
      setAttempt(null);
      navigate("shipping");
    });
  }
  async function submitOrder(method, scenario) {
    await run(async () => {
      if (!quote) throw new Error("Review your shipping and quote first.");
      const payload = {
        quoteId: quote.id,
        quoteDigest: quote.digest,
        shippingAddress: shipping,
        paymentMethod: method,
        scenario,
      };
      const identity = JSON.stringify(payload);
      const current =
        attempt?.identity === identity
          ? attempt
          : { identity, key: crypto.randomUUID(), payload };
      setAttempt(current);
      const result = await api("/orders", {
        method: "POST",
        body: current.payload,
        headers: { "Idempotency-Key": current.key },
      });
      setOrder(result);
      await loadCart();
      navigate("confirmation");
    });
  }
  const count = cart.reduce((n, c) => n + c.quantity, 0);
  return (
    <>
      <header>
        <button
          className="brand"
          onClick={() => navigate("home")}
          aria-label="NP Computers home"
        >
          <span className="monogram">NP</span>
          <span>
            COMPUTERS<small>Technology for your next chapter</small>
          </span>
        </button>
        <nav aria-label="Main navigation">
          {page !== "home" && (
            <button onClick={() => navigate("home")}>Home</button>
          )}
          <button onClick={() => navigate("catalog")}>Shop</button>
          <button onClick={() => navigate("contact")}>Contact</button>
          {user && (
            <>
              <button onClick={() => navigate("profile")}>Profile</button>
              <button onClick={() => navigate("orders")}>Orders</button>
              {user.role !== "CUSTOMER" && (
                <button onClick={() => navigate("staff")}>Staff</button>
              )}
            </>
          )}
          <button onClick={() => (user ? navigate("cart") : navigate("login"))}>
            Cart <span className="badge">{count}</span>
          </button>
          <button
            className="dark"
            onClick={() =>
              user
                ? run(async () => {
                    await signOut();
                    setUser(null);
                    navigate("home");
                  })
                : navigate("login")
            }
          >
            {user ? "Log out" : "Log in"}
          </button>
        </nav>
      </header>
      <div className="demo-banner">
        DEMO STORE · Simulated payments only · Illustrative products and prices
        · U.S. shipping
      </div>
      <main>
        {error && (
          <div className="notice error" role="alert">
            {error}
          </div>
        )}
        {message && (
          <div className="notice" role="status">
            {message}
          </div>
        )}
        {!ready ? (
          <p>Opening the store…</p>
        ) : (
          <>
            {page === "home" && (
              <>
                <section className="hero">
                  <div>
                    <p className="eyebrow">
                      NEW POSSIBILITIES. PRACTICAL PRICES.
                    </p>
                    <h1>
                      Your next computer.
                      <br />
                      <em>Your way.</em>
                    </h1>
                    <p>
                      Find new and used computers for work, study and everything
                      in between. Pair them with new accessories from brands you
                      know.
                    </p>
                    <button
                      className="primary"
                      onClick={() => navigate("catalog")}
                    >
                      Explore the collection <span>→</span>
                    </button>
                    <p className="quiet">Apple · Lenovo · HP</p>
                  </div>
                  <div className="computer-art" aria-hidden="true">
                    <div className="screen">
                      <div className="orb" />
                      <span>NP</span>
                    </div>
                    <div className="stand" />
                    <div className="keyboard" />
                  </div>
                </section>
                <section className="value-grid">
                  <article>
                    <span>01</span>
                    <h2>A fit for every desk</h2>
                    <p>
                      Desktops, laptops and tablets with clear specifications.
                    </p>
                  </article>
                  <article>
                    <span>02</span>
                    <h2>New or thoughtfully used</h2>
                    <p>
                      Choose the condition that suits your needs and budget.
                    </p>
                  </article>
                  <article>
                    <span>03</span>
                    <h2>Make it a complete setup</h2>
                    <p>
                      Find new keyboards, docks and mice to finish your
                      workspace.
                    </p>
                  </article>
                </section>
              </>
            )}
            {page === "catalog" && (
              <Catalog
                busy={busy}
                run={run}
                add={async (p, qty) => {
                  if (!(await requireUser())) return;
                  await run(async () => {
                    await api("/cart/items/" + p.id, {
                      method: "PUT",
                      body: {
                        quantity:
                          (cart.find((c) => c.product.id === p.id)?.quantity ||
                            0) + qty,
                      },
                    });
                    await loadCart();
                    setMessage(`${p.model} added to your cart.`);
                  });
                }}
              />
            )}
            {["login", "register", "recovery", "reset"].includes(page) && (
              <AuthForm
                page={page}
                busy={busy}
                navigate={navigate}
                run={run}
                onLogin={(r) => {
                  setToken(r.accessToken);
                  setUser(r.user);
                  navigate("catalog");
                }}
                message={setMessage}
              />
            )}
            {page === "profile" &&
              (user ? (
                <ProfileForm
                  user={user}
                  run={run}
                  busy={busy}
                  save={async (p) => {
                    await api("/me/profile", { method: "PUT", body: p });
                    setProfile(p);
                    navigate("home");
                    setMessage("Profile saved.");
                  }}
                  cancel={() =>
                    navigate(previous === "login" ? "home" : previous)
                  }
                />
              ) : (
                <SignIn navigate={navigate} />
              ))}
            {page === "cart" &&
              (user ? (
                <>
                  <Title
                    title="Your cart"
                    subtitle="Stock is checked again when you place your order."
                  />
                  <Cart
                    cart={cart}
                    busy={busy}
                    change={(id, n) =>
                      run(async () => {
                        await api("/cart/items/" + id, {
                          method: n ? "PUT" : "DELETE",
                          body: n ? { quantity: n } : undefined,
                        });
                        await loadCart();
                        setQuote(null);
                        setAttempt(null);
                      })
                    }
                  />
                  {cart.length > 0 && (
                    <div className="actions">
                      <button
                        className="primary"
                        disabled={busy}
                        onClick={toShipping}
                      >
                        Place order
                      </button>
                      <button onClick={() => navigate("catalog")}>
                        Continue shopping
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <SignIn navigate={navigate} />
              ))}
            {page === "shipping" &&
              (user ? (
                <>
                  <Title
                    title="Shipping details"
                    subtitle="Review your items and choose the shipping address for this order."
                  />
                  <Cart
                    cart={cart}
                    busy={busy}
                    change={(id, n) =>
                      run(async () => {
                        await api("/cart/items/" + id, {
                          method: n ? "PUT" : "DELETE",
                          body: n ? { quantity: n } : undefined,
                        });
                        await loadCart();
                        setQuote(null);
                        setAttempt(null);
                      })
                    }
                  />
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      run(async () => {
                        const q = await api("/checkout/quote", {
                          method: "POST",
                          body: { shippingAddress: shipping },
                        });
                        setQuote(q);
                        setAttempt(null);
                        navigate("payment");
                      });
                    }}
                  >
                    <AddressForm
                      title="Order shipping address"
                      value={shipping}
                      onChange={(v) => {
                        setShipping(v);
                        setQuote(null);
                        setAttempt(null);
                      }}
                    />
                    {profile && (
                      <button
                        type="button"
                        onClick={() => setShipping(profile.shipping)}
                      >
                        Use saved shipping address
                      </button>
                    )}
                    <div className="actions">
                      <button
                        className="primary"
                        disabled={busy || !cart.length}
                      >
                        Pay for order
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (
                            confirm("Clear your cart and return to shopping?")
                          )
                            run(async () => {
                              await api("/cart", { method: "DELETE" });
                              await loadCart();
                              navigate("catalog");
                            });
                        }}
                      >
                        Cancel order
                      </button>
                    </div>
                  </form>
                </>
              ) : (
                <SignIn navigate={navigate} />
              ))}
            {page === "payment" &&
              (quote ? (
                <Payment
                  quote={quote}
                  busy={busy}
                  submit={submitOrder}
                  back={() => navigate("shipping")}
                  cancel={() => {
                    if (confirm("Clear your cart and return to shipping?"))
                      run(async () => {
                        await api("/cart", { method: "DELETE" });
                        await loadCart();
                        setQuote(null);
                        setAttempt(null);
                        navigate("shipping");
                      });
                  }}
                />
              ) : (
                <>
                  <p>Review a new shipping quote before payment.</p>
                  <button onClick={() => navigate("cart")}>
                    Return to cart
                  </button>
                </>
              ))}
            {page === "confirmation" &&
              (order ? (
                <OrderDetail
                  order={order}
                  retry={() => {
                    setQuote(null);
                    setAttempt(null);
                    navigate("cart");
                  }}
                />
              ) : (
                <Orders run={run} open={(o) => setOrder(o)} busy={busy} />
              ))}
            {page === "orders" &&
              (user ? (
                <Orders
                  run={run}
                  busy={busy}
                  open={(o) => {
                    setOrder(o);
                    navigate("confirmation");
                  }}
                />
              ) : (
                <SignIn navigate={navigate} />
              ))}
            {page === "contact" && (
              <ContactPage
                run={run}
                busy={busy}
                message={setMessage}
                navigate={navigate}
              />
            )}
            {page === "staff" &&
              (user && user.role !== "CUSTOMER" ? (
                <Staff user={user} run={run} busy={busy} />
              ) : (
                <p>This page requires a staff account.</p>
              ))}
          </>
        )}
      </main>
      <footer>
        <div>
          <strong>NP Computers Inc.</strong>
          <p>New and used computers. New accessories. U.S. customers.</p>
        </div>
        <p>
          Demonstration application
          <br />
          No real payments or external email.
        </p>
      </footer>
    </>
  );
}
function Title({ title, subtitle }) {
  return (
    <div className="page-title">
      <p className="eyebrow">NP COMPUTERS</p>
      <h1>{title}</h1>
      {subtitle && <p>{subtitle}</p>}
    </div>
  );
}
function SignIn({ navigate }) {
  return (
    <>
      <Title
        title="Welcome back"
        subtitle="Log in to manage your account and orders."
      />
      <button className="primary" onClick={() => navigate("login")}>
        Log in
      </button>
    </>
  );
}
function Catalog({ add, busy, run }) {
  const [items, setItems] = useState([]),
    [filters, setFilters] = useState({
      q: "",
      category: "",
      manufacturer: "",
      condition: "",
    }),
    [page, setPage] = useState(0),
    [total, setTotal] = useState(0),
    [detail, setDetail] = useState(null),
    [qty, setQty] = useState(1);
  async function load() {
    const q = new URLSearchParams({ ...filters, page, size: 12 });
    const d = await api("/products?" + q);
    setItems(d.items);
    setTotal(d.total);
  }
  useEffect(() => {
    run(load);
  }, [page, filters.category, filters.manufacturer, filters.condition]);
  return (
    <>
      <Title
        title="Find your next setup"
        subtitle="Explore computers and accessories. All specifications and prices shown are illustrative."
      />
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          run(load);
        }}
      >
        <Field
          label="Search"
          value={filters.q}
          placeholder="Model, SKU or description"
          onChange={(e) => setFilters({ ...filters, q: e.target.value })}
        />
        {[
          ["category", "Category", ["PC", "LAPTOP", "TABLET", "ACCESSORY"]],
          ["manufacturer", "Brand", ["Apple", "Lenovo", "HP"]],
          ["condition", "Condition", ["NEW", "USED"]],
        ].map(([k, label, opts]) => (
          <Select
            key={k}
            label={label}
            value={filters[k]}
            onChange={(e) => {
              setPage(0);
              setFilters({ ...filters, [k]: e.target.value });
            }}
          >
            <option value="">All {label.toLowerCase()}s</option>
            {opts.map((o) => (
              <option key={o}>{o}</option>
            ))}
          </Select>
        ))}
        <button className="dark" disabled={busy}>
          Search
        </button>
      </form>
      <p className="quiet">{total} products · Demo catalog</p>
      <section className="products">
        {items.map((p) => (
          <article key={p.id} className="product-card">
            <div
              className={"product-visual " + p.category.toLowerCase()}
              aria-hidden="true"
            >
              <span>
                {p.category === "ACCESSORY"
                  ? "⌨"
                  : p.category === "TABLET"
                    ? "▯"
                    : "▰"}
              </span>
              <small>{p.manufacturer}</small>
            </div>
            <div className="product-copy">
              <div className="tag">
                {p.condition === "USED" ? "Used" : "New"} ·{" "}
                {p.category.toLowerCase()}
              </div>
              <h2>{p.model}</h2>
              <p>
                {p.ram ? `${p.ram} · ${p.storage}` : "Complete your workspace"}
              </p>
              <div className="product-bottom">
                <strong>{money(p.price)}</strong>
                <button
                  onClick={() => {
                    setDetail(p);
                    setQty(1);
                  }}
                >
                  View details
                </button>
              </div>
            </div>
          </article>
        ))}
      </section>
      {!items.length && <p>No products match these filters.</p>}
      <div className="actions">
        <button disabled={page === 0 || busy} onClick={() => setPage(page - 1)}>
          Previous
        </button>
        <span>Page {page + 1}</span>
        <button
          disabled={(page + 1) * 12 >= total || busy}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
      {detail && (
        <section className="detail" aria-label="Product details">
          <button className="close" onClick={() => setDetail(null)}>
            Close details
          </button>
          <h2>{detail.model}</h2>
          <p>{detail.description}</p>
          <dl>
            {[
              "sku",
              "manufacturer",
              "condition",
              "cpu",
              "ram",
              "storage",
              "gpu",
              "os",
              "stock",
            ]
              .filter((k) => detail[k] != null)
              .map((k) => (
                <React.Fragment key={k}>
                  <dt>{k.toUpperCase()}</dt>
                  <dd>{detail[k]}</dd>
                </React.Fragment>
              ))}
          </dl>
          <strong>{money(detail.price)}</strong>
          <Field
            label="Quantity"
            type="number"
            min="1"
            max={detail.stock}
            value={qty}
            onChange={(e) => setQty(Number(e.target.value))}
          />
          <button
            className="primary"
            disabled={
              busy || qty < 1 || qty > detail.stock || !Number.isInteger(qty)
            }
            onClick={() => add(detail, qty)}
          >
            Add to cart
          </button>
        </section>
      )}
    </>
  );
}
function AuthForm({ page, busy, navigate, run, onLogin, message }) {
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [confirmation, setConfirmation] = useState(""),
    [visible, setVisible] = useState(false),
    [token, setResetToken] = useState(
      new URLSearchParams(location.search).get("reset") || "",
    );
  async function submit(e) {
    e.preventDefault();
    await run(async () => {
      if (page === "register") {
        await api("/auth/register", {
          method: "POST",
          body: { email, password, confirmPassword: confirmation },
        });
        navigate("login");
        message("Account created. Log in to complete your profile.");
      } else if (page === "recovery") {
        const r = await api("/auth/recovery", {
          method: "POST",
          body: { email },
        });
        message(r.message);
      } else if (page === "reset") {
        await api("/auth/reset", { method: "POST", body: { token, password } });
        navigate("login");
        message("Password changed. Please log in.");
      } else
        onLogin(
          await api("/auth/login", {
            method: "POST",
            body: { email, password },
          }),
        );
    });
  }
  return (
    <section className="form-shell">
      <Title
        title={
          {
            login: "Welcome back",
            register: "Create your account",
            recovery: "Recover your password",
            reset: "Choose a new password",
          }[page]
        }
        subtitle="Your account connects your profile, cart and order history."
      />
      <form onSubmit={submit}>
        {page !== "reset" && (
          <Field
            label="Email address"
            required
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        )}{" "}
        {page === "reset" && (
          <Field
            label="Reset token from the local mail sink"
            required
            value={token}
            onChange={(e) => setResetToken(e.target.value)}
          />
        )}{" "}
        {page !== "recovery" && (
          <>
            <Field
              label="Password"
              required
              type={visible ? "text" : "password"}
              minLength={12}
              maxLength={128}
              autoComplete={
                page === "login" ? "current-password" : "new-password"
              }
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <label className="check">
              <input
                type="checkbox"
                checked={visible}
                onChange={(e) => setVisible(e.target.checked)}
              />
              Show password
            </label>
            <small>
              Use 12 to 128 characters. Spaces and symbols are welcome.
            </small>
          </>
        )}{" "}
        {page === "register" && (
          <Field
            label="Reenter password"
            required
            type={visible ? "text" : "password"}
            minLength={12}
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
          />
        )}
        <div className="actions">
          <button
            className="primary"
            disabled={
              busy || (page === "register" && password !== confirmation)
            }
          >
            {busy
              ? "Please wait…"
              : {
                  login: "Log in",
                  register: "Create account",
                  recovery: "Generate recovery instructions",
                  reset: "Save new password",
                }[page]}
          </button>
          <button type="button" onClick={() => navigate("home")}>
            Cancel
          </button>
        </div>
      </form>
      {page === "login" && (
        <div className="actions">
          <button onClick={() => navigate("register")}>Create account</button>
          <button onClick={() => navigate("recovery")}>Recover password</button>
          <button onClick={() => navigate("reset")}>Use reset link</button>
        </div>
      )}
    </section>
  );
}
function ProfileForm({ user, run, busy, save, cancel }) {
  const [value, setValue] = useState({
    kind: "INDIVIDUAL",
    businessName: "",
    primary: { ...emptyContact(), email: user.email },
    alternate: null,
    mailing: emptyAddress(),
    shipping: emptyAddress(),
    version: 0,
  });
  useEffect(() => {
    api("/me/profile")
      .then(setValue)
      .catch((e) => {
        if (e.status !== 404)
          run(() => {
            throw e;
          });
      });
  }, []);
  const set = (k, v) => setValue({ ...value, [k]: v });
  return (
    <>
      <Title
        title="Your profile"
        subtitle={`Account ${user.accountNumber} · Your shipping details stay private.`}
      />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          run(() => save(value));
        }}
      >
        <Select
          label="Customer type"
          value={value.kind}
          onChange={(e) => set("kind", e.target.value)}
        >
          <option value="INDIVIDUAL">Individual</option>
          <option value="BUSINESS">Business</option>
        </Select>
        {value.kind === "BUSINESS" && (
          <Field
            label="Business name"
            required
            value={value.businessName}
            onChange={(e) => set("businessName", e.target.value)}
          />
        )}
        <ContactForm
          title="Primary contact"
          value={value.primary}
          onChange={(v) => set("primary", v)}
        />
        <label className="check">
          <input
            type="checkbox"
            checked={!!value.alternate}
            onChange={(e) =>
              set("alternate", e.target.checked ? emptyContact() : null)
            }
          />
          Add alternate contact
        </label>
        {value.alternate && (
          <ContactForm
            title="Alternate contact"
            value={value.alternate}
            onChange={(v) => set("alternate", v)}
          />
        )}
        <AddressForm
          title="Mailing address"
          value={value.mailing}
          onChange={(v) => set("mailing", v)}
        />
        <button
          type="button"
          onClick={() => set("shipping", { ...value.mailing })}
        >
          Same as Address
        </button>
        <AddressForm
          title="Shipping address"
          value={value.shipping}
          onChange={(v) => set("shipping", v)}
        />
        <div className="actions">
          <button className="primary" disabled={busy || !profileValid(value)}>
            Save profile
          </button>
          <button type="button" onClick={cancel}>
            Cancel
          </button>
        </div>
      </form>
    </>
  );
}
function Cart({ cart, busy, change }) {
  return cart.length ? (
    <>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>Price</th>
              <th>Quantity</th>
              <th>Subtotal</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {cart.map((c) => (
              <tr key={c.product.id}>
                <td>
                  <strong>{c.product.model}</strong>
                  <small>
                    {c.product.sku} · {c.product.condition}
                  </small>
                </td>
                <td>{money(c.product.price)}</td>
                <td>
                  <input
                    aria-label={`Quantity for ${c.product.model}`}
                    className="quantity"
                    type="number"
                    min="1"
                    max={c.product.stock}
                    defaultValue={c.quantity}
                    key={c.quantity}
                    disabled={busy}
                    onBlur={(e) => {
                      const n = Number(e.target.value);
                      if (Number.isInteger(n) && n > 0 && n !== c.quantity)
                        change(c.product.id, n);
                    }}
                  />
                </td>
                <td>{money(c.product.price * c.quantity)}</td>
                <td>
                  <button
                    disabled={busy}
                    onClick={() => change(c.product.id, 0)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="cart-total">
        Item subtotal{" "}
        <strong>
          {money(cart.reduce((n, c) => n + c.quantity * c.product.price, 0))}
        </strong>
      </p>
    </>
  ) : (
    <p>Your cart is empty. Explore the collection to get started.</p>
  );
}
function Payment({ quote, busy, submit, back, cancel }) {
  const [method, setMethod] = useState("SIMULATED_CARD"),
    [scenario, setScenario] = useState("APPROVE");
  const q = quote.quote;
  return (
    <>
      <Title
        title="Review and pay"
        subtitle="This is a simulated checkout. Never enter real card or wallet details."
      />
      <section className="checkout-summary">
        <h2>Order review</h2>
        {q.items.map((i) => (
          <p key={i.productId}>
            {i.sku} × {i.quantity}
            <strong>{money(i.quantity * i.unitPrice)}</strong>
          </p>
        ))}
        <p>
          Shipping <strong>{money(q.shipping)}</strong>
        </p>
        <p>
          Demo tax <strong>{money(q.tax)}</strong>
        </p>
        <p className="total">
          Total <strong>{money(q.total)}</strong>
        </p>
        <small>
          Demo policy: flat $10 shipping and zero tax. Quote expires after 10
          minutes.
        </small>
      </section>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(method, scenario);
        }}
      >
        <Select
          label="Simulated payment method"
          value={method}
          onChange={(e) => setMethod(e.target.value)}
        >
          <option value="SIMULATED_CARD">Demo card</option>
          <option value="SIMULATED_WALLET">Demo wallet</option>
        </Select>
        <Select
          label="Demo result"
          value={scenario}
          onChange={(e) => setScenario(e.target.value)}
        >
          <option value="APPROVE">Approve payment</option>
          <option value="DECLINE">Decline payment</option>
        </Select>
        <div className="actions">
          <button className="primary" disabled={busy}>
            {busy ? "Submitting…" : "Complete order"}
          </button>
          <button type="button" onClick={back}>
            Shipment info
          </button>
          <button type="button" onClick={cancel}>
            Cancel order
          </button>
        </div>
      </form>
    </>
  );
}
function OrderDetail({ order, retry }) {
  const a = order.shippingAddress;
  return (
    <>
      <Title
        title={
          order.status === "PAYMENT_FAILED" ? "Payment declined" : "Your order"
        }
        subtitle={order.orderNumber}
      />
      <p className="status-pill">{order.status}</p>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th>Quantity</th>
              <th>Unit price</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            {order.items.map((i) => (
              <tr key={i.productId}>
                <td>
                  {i.sku}
                  <small>{i.description}</small>
                </td>
                <td>{i.quantity}</td>
                <td>{money(i.unitPrice)}</td>
                <td>{money(i.quantity * i.unitPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="grid">
        <section>
          <h2>Shipping</h2>
          <p>
            {a.line1}
            <br />
            {a.line2}
            <br />
            {a.city}, {a.state} {a.zip}
          </p>
        </section>
        <section>
          <h2>Payment</h2>
          <p>
            {order.payment.method}
            <br />
            {order.payment.status}
            <br />
            {order.payment.reference}
          </p>
        </section>
      </div>
      <p>
        Subtotal {money(order.subtotal)} · Shipping {money(order.shipping)} ·
        Demo tax {money(order.tax)}
      </p>
      <h2>Total {money(order.total)}</h2>
      <p>{order.notification}</p>
      {order.tracking && <p>Tracking: {order.tracking}</p>}
      {order.status === "PAYMENT_FAILED" && (
        <button className="primary" onClick={retry}>
          Review cart and try again
        </button>
      )}
    </>
  );
}
function Orders({ run, open, busy }) {
  const [orders, setOrders] = useState([]),
    [page, setPage] = useState(0),
    [total, setTotal] = useState(0);
  useEffect(() => {
    run(async () => {
      const d = await api("/orders?page=" + page);
      setOrders(d.items);
      setTotal(d.total);
    });
  }, [page]);
  return (
    <>
      <Title
        title="Order history"
        subtitle="Open an order to see its shipping and payment details."
      />
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Order</th>
              <th>Date</th>
              <th>Status</th>
              <th>Total</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.orderNumber}</td>
                <td>{new Date(o.createdAt).toLocaleDateString()}</td>
                <td>{o.status}</td>
                <td>{money(o.total)}</td>
                <td>
                  <button
                    disabled={busy}
                    onClick={() =>
                      run(async () => open(await api("/orders/" + o.id)))
                    }
                  >
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!orders.length && <p>No orders yet.</p>}
      <div className="actions">
        <button disabled={page === 0} onClick={() => setPage(page - 1)}>
          Previous
        </button>
        <span>Page {page + 1}</span>
        <button
          disabled={(page + 1) * 20 >= total}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
    </>
  );
}
function ContactPage({ run, busy, message, navigate }) {
  const [v, set] = useState({
    email: "",
    phone: "",
    reason: "Order Inquiry",
    message: "",
  });
  return (
    <>
      <Title
        title="Get in touch"
        subtitle="Demo contact details carried forward from the original requirements. These details are unverified."
      />
      <div className="grid">
        <section>
          <h2>Snail Mail Us</h2>
          <p>
            NPC Customer Service
            <br />
            1234 S. NPC Way
            <br />
            Overland Park, KS 66251
          </p>
          <h2>Call Us</h2>
          <p>+1 913-111-2222, option 4</p>
        </section>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              const r = await api("/contact-inquiries", {
                method: "POST",
                body: v,
              });
              message(r.message + " Reference: " + r.reference);
              set({ ...v, message: "" });
            });
          }}
        >
          <h2>Email Us</h2>
          <Field
            label="Email"
            required
            type="email"
            value={v.email}
            onChange={(e) => set({ ...v, email: e.target.value })}
          />
          <Field
            label="Telephone"
            required
            pattern="[+0-9 ()\-]{7,40}"
            value={v.phone}
            onChange={(e) => set({ ...v, phone: e.target.value })}
          />
          <Select
            label="Reason for contact"
            value={v.reason}
            onChange={(e) => set({ ...v, reason: e.target.value })}
          >
            {[
              "Order Inquiry",
              "Product Inquiry",
              "Support",
              "Cancellation",
              "Other",
            ].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </Select>
          <label className="field">
            Message
            <textarea
              required
              maxLength={500}
              value={v.message}
              onChange={(e) => set({ ...v, message: e.target.value })}
            />
            <small>{v.message.length}/500 characters</small>
          </label>
          <div className="actions">
            <button className="primary" disabled={busy}>
              Submit
            </button>
            <button type="button" onClick={() => navigate("home")}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </>
  );
}
const productDefaults = {
  sku: "",
  manufacturer: "",
  model: "",
  category: "LAPTOP",
  condition: "NEW",
  cpu: "",
  ram: "",
  storage: "",
  gpu: "",
  os: "",
  price: 0,
  description: "",
  active: true,
  version: 0,
};
function Staff({ user, run, busy }) {
  const [tab, setTab] = useState("orders"),
    [data, setData] = useState([]),
    [edit, setEdit] = useState(null),
    [selected, setSelected] = useState(null),
    [tracking, setTracking] = useState(""),
    [page, setPage] = useState(0);
  const tabs =
    user.role === "ADMIN"
      ? ["orders", "products", "users", "inventory", "audit", "mail-sink"]
      : ["orders"];
  async function load() {
    const path = {
      orders: "/orders?page=" + page,
      products: "/admin/products?page=" + page,
      users: "/admin/users?page=" + page,
      inventory: "/admin/inventory/replenishments",
      audit: "/admin/audit?page=" + page,
      "mail-sink": "/admin/mail-sink",
    }[tab];
    const d = await api(path);
    setData(d.items || d);
  }
  useEffect(() => {
    run(load);
  }, [tab, page]);
  function editProduct(p) {
    const v = { ...productDefaults };
    for (const k of Object.keys(v)) v[k] = p[k] ?? v[k];
    setEdit({ id: p.id, ...v });
  }
  return (
    <>
      <Title
        title="Staff workspace"
        subtitle={`${user.role} · Actions are checked and audited by the server.`}
      />
      <div className="actions">
        {tabs.map((t) => (
          <button
            className={t === tab ? "dark" : ""}
            key={t}
            onClick={() => {
              setTab(t);
              setPage(0);
              setEdit(null);
              setSelected(null);
            }}
          >
            {t}
          </button>
        ))}
      </div>
      {tab === "products" && (
        <button
          className="primary"
          onClick={() => setEdit({ ...productDefaults })}
        >
          New product
        </button>
      )}
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Record</th>
              <th>Details</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.map((r, i) => (
              <tr key={r.id || i}>
                <td>
                  {r.orderNumber ||
                    r.sku ||
                    r.email ||
                    r.action ||
                    r.subject ||
                    r.id}
                </td>
                <td>
                  {tab === "orders"
                    ? `${r.status} · ${money(r.total)}`
                    : tab === "products"
                      ? `${r.model} · Stock ${r.stock} · ${r.active ? "Active" : "Inactive"}`
                      : tab === "users"
                        ? `${r.role} · ${r.enabled ? "Enabled" : "Disabled"}`
                        : tab === "mail-sink"
                          ? r.body
                          : JSON.stringify(r)}
                </td>
                <td>
                  {tab === "products" && (
                    <>
                      <button onClick={() => editProduct(r)}>Edit</button>
                      <button
                        onClick={() => {
                          const delta = Number(
                            prompt(
                              "Stock adjustment quantity (negative to reduce):",
                            ),
                          );
                          if (!Number.isInteger(delta) || !delta) return;
                          const reason = prompt("Reason for adjustment:");
                          if (reason)
                            run(async () => {
                              await api("/admin/inventory/adjustments", {
                                method: "POST",
                                body: {
                                  productId: r.id,
                                  delta,
                                  reason,
                                  reference: crypto.randomUUID(),
                                },
                              });
                              await load();
                            });
                        }}
                      >
                        Adjust stock
                      </button>
                    </>
                  )}
                  {tab === "users" && r.id !== user.id && (
                    <>
                      <Select
                        label="Role"
                        value={r.role}
                        onChange={(e) => {
                          const role = e.target.value;
                          if (
                            confirm("Change role and revoke existing sessions?")
                          )
                            run(async () => {
                              await api("/admin/users/" + r.id + "/roles", {
                                method: "POST",
                                body: { role },
                              });
                              await load();
                            });
                        }}
                      >
                        {["CUSTOMER", "ADMIN", "SALES", "SUPPORT"].map((x) => (
                          <option key={x}>{x}</option>
                        ))}
                      </Select>
                      <button
                        onClick={() =>
                          run(async () => {
                            await api("/admin/users/" + r.id + "/enabled", {
                              method: "POST",
                              body: { enabled: !r.enabled },
                            });
                            await load();
                          })
                        }
                      >
                        {r.enabled ? "Disable" : "Enable"}
                      </button>
                    </>
                  )}
                  {tab === "orders" && (
                    <button
                      onClick={() =>
                        run(async () => {
                          setSelected(await api("/orders/" + r.id));
                          setTracking("");
                        })
                      }
                    >
                      Open order
                    </button>
                  )}
                  {tab === "inventory" && r.status !== "RECEIVED" && (
                    <button
                      onClick={() =>
                        run(async () => {
                          await api("/admin/inventory/receipts", {
                            method: "POST",
                            body: {
                              requestId: r.id,
                              reference: crypto.randomUUID(),
                            },
                          });
                          await load();
                        })
                      }
                    >
                      Receive stock
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!["inventory", "mail-sink"].includes(tab) && (
        <div className="actions">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </button>
          <span>Page {page + 1}</span>
          <button
            disabled={data.length === 0}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      )}
      {selected && (
        <>
          <OrderDetail order={selected} retry={() => {}} />
          {user.role !== "SUPPORT" &&
            ["CONFIRMED", "PROCESSING"].includes(selected.status) && (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  run(async () => {
                    const status =
                      selected.status === "CONFIRMED"
                        ? "PROCESSING"
                        : "SHIPPED";
                    setSelected(
                      await api("/staff/orders/" + selected.id + "/status", {
                        method: "PATCH",
                        body: { status, tracking, version: selected.version },
                      }),
                    );
                    await load();
                  });
                }}
              >
                {selected.status === "PROCESSING" && (
                  <Field
                    label="Tracking reference"
                    required
                    value={tracking}
                    onChange={(e) => setTracking(e.target.value)}
                  />
                )}
                <button className="primary" disabled={busy}>
                  Mark{" "}
                  {selected.status === "CONFIRMED" ? "processing" : "shipped"}
                </button>
              </form>
            )}
        </>
      )}
      {edit && (
        <form
          className="detail"
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              const { id, ...body } = edit;
              await api("/admin/products" + (id ? "/" + id : ""), {
                method: id ? "PUT" : "POST",
                body: { ...body, price: Number(body.price) },
              });
              setEdit(null);
              await load();
            });
          }}
        >
          <h2>{edit.id ? "Edit product" : "New product"}</h2>
          <div className="grid">
            {[
              "sku",
              "manufacturer",
              "model",
              "cpu",
              "ram",
              "storage",
              "gpu",
              "os",
            ].map((k) => (
              <Field
                key={k}
                label={k.toUpperCase()}
                required={
                  ["sku", "manufacturer", "model"].includes(k) ||
                  (edit.category !== "ACCESSORY" &&
                    ["cpu", "ram", "storage", "os"].includes(k))
                }
                value={edit[k]}
                onChange={(e) => setEdit({ ...edit, [k]: e.target.value })}
              />
            ))}
          </div>
          <div className="grid">
            <Select
              label="Category"
              value={edit.category}
              onChange={(e) => setEdit({ ...edit, category: e.target.value })}
            >
              {["PC", "LAPTOP", "TABLET", "ACCESSORY"].map((x) => (
                <option key={x}>{x}</option>
              ))}
            </Select>
            <Select
              label="Condition"
              value={edit.condition}
              onChange={(e) => setEdit({ ...edit, condition: e.target.value })}
            >
              {["NEW", "USED"].map((x) => (
                <option key={x}>{x}</option>
              ))}
            </Select>
            <Field
              label="USD price"
              required
              type="number"
              min="0.01"
              step="0.01"
              value={edit.price}
              onChange={(e) => setEdit({ ...edit, price: e.target.value })}
            />
          </div>
          <label className="field">
            Description
            <textarea
              required
              maxLength={2000}
              value={edit.description}
              onChange={(e) =>
                setEdit({ ...edit, description: e.target.value })
              }
            />
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={edit.active}
              onChange={(e) => setEdit({ ...edit, active: e.target.checked })}
            />
            Active
          </label>
          <div className="actions">
            <button className="primary" disabled={busy}>
              Save product
            </button>
            <button type="button" onClick={() => setEdit(null)}>
              Cancel
            </button>
          </div>
        </form>
      )}
    </>
  );
}
createRoot(document.getElementById("root")).render(<App />);
