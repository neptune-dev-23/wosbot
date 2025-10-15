import { FiShoppingBag, FiTag, FiDollarSign } from "react-icons/fi";

const SHOP_OVERVIEW = [
  { label: "Daily Deals", value: "6 available", trend: "Refresh in 2h" },
  { label: "Alliance Credits", value: "14,500", trend: "Plan bulk purchase" },
  { label: "VIP Store", value: "Level 13", trend: "Unlocks new hero shards" },
  { label: "Limited Offers", value: "2", trend: "Ends at reset" },
];

const FEATURED_BUNDLES = [
  { name: "War Prep Pack", contents: ["x10 speed-ups", "x5 rally boosts", "x50 steel"] },
  { name: "Growth Pack", contents: ["x15 training speed-ups", "x20 hero XP", "x30 gas"] },
  { name: "Alliance Support", contents: ["x40 donation tokens", "x10 construction boosts"] },
];

const SHOP_ACTIONS = [
  { title: "Sync with Alliance", detail: "Coordinate purchases that benefit group buffs." },
  { title: "Price Tracking", detail: "Log discounts to catch repeating offers." },
  { title: "Budget Planning", detail: "Balance premium currency with event rewards." },
];

const ShopPage = () => (
  <div className="view active" id="shopView">
    <div className="header">
      <h1>
        <FiShoppingBag aria-hidden="true" className="header-icon" size={24} />
        <span>Shop Overview</span>
      </h1>
      <p className="header-subtitle">
        Monitor store rotations, highlight high-impact bundles, and manage alliance currency spend.
      </p>
    </div>
    <div className="content-container page-content">
      <section className="page-section">
        <h2>Store Snapshot</h2>
        <div className="stat-grid">
          {SHOP_OVERVIEW.map((entry) => (
            <div className="stat-card" key={entry.label}>
              <h3>{entry.label}</h3>
              <span className="stat-value">{entry.value}</span>
              <span className="stat-trend">{entry.trend}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Featured Bundles</h2>
        <div className="page-grid">
          {FEATURED_BUNDLES.map((bundle) => (
            <div className="page-panel" key={bundle.name}>
              <h3>{bundle.name}</h3>
              <ul>
                {bundle.contents.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Shopping Notes</h2>
        <div className="list-grid">
          {SHOP_ACTIONS.map((item) => (
            <div className="list-card" key={item.title}>
              <strong>{item.title}</strong>
              <span>{item.detail}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="page-section">
        <h2>Spending Strategy</h2>
        <div className="page-grid">
          <div className="page-panel">
            <h3>
              <FiTag aria-hidden="true" /> Discount Tracker
            </h3>
            <p>Log recurring discounts and coordinate purchases during optimal windows.</p>
          </div>
          <div className="page-panel">
            <h3>
              <FiDollarSign aria-hidden="true" /> Currency Planning
            </h3>
            <p>Balance premium currency usage with free-to-play gains from events and milestones.</p>
          </div>
        </div>
      </section>
    </div>
  </div>
);

export default ShopPage;
