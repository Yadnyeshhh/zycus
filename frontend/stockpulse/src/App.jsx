import React, { useState, useEffect, useCallback } from 'react';
import {
  fetchProducts,
  fetchPricingSuggestions,
  fetchReorderSuggestions,
  simulateOrder,
  updateStock,
  requestPricingSuggestion,
  requestReorderSuggestion,
  decidePricingSuggestion,
  decideReorderSuggestion,
  fetchActiveStrategy,
  switchStrategy
} from './services/api';
import './App.css';

export default function App() {
  const [products, setProducts] = useState([]);
  const [pricingSuggestions, setPricingSuggestions] = useState([]);
  const [reorderSuggestions, setReorderSuggestions] = useState([]);
  const [activeStrategy, setActiveStrategy] = useState('RULE_BASED');
  const [availableStrategies, setAvailableStrategies] = useState(['RULE_BASED', 'AI']);
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const showToast = (message, type = 'info') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      setError(null);
      const [prods, pricing, reorder, stratInfo] = await Promise.all([
        fetchProducts(
          statusFilter !== 'ALL' ? statusFilter : null,
          categoryFilter !== 'ALL' ? categoryFilter : null
        ),
        fetchPricingSuggestions('PENDING'),
        fetchReorderSuggestions('PENDING'),
        fetchActiveStrategy().catch(() => ({ activeStrategy: 'RULE_BASED', availableStrategies: ['RULE_BASED', 'AI'] }))
      ]);

      setProducts(prods);
      setPricingSuggestions(pricing);
      setReorderSuggestions(reorder);
      if (stratInfo && stratInfo.activeStrategy) {
        setActiveStrategy(stratInfo.activeStrategy);
        if (stratInfo.availableStrategies) setAvailableStrategies(stratInfo.availableStrategies);
      }
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Data load error:', err);
      setError(err.message || 'Failed to connect to backend server at http://localhost:8080');
    } finally {
      if (!silent) setLoading(false);
    }
  }, [categoryFilter, statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Polling loop
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => {
      loadData(true);
    }, 3000);
    return () => clearInterval(interval);
  }, [autoRefresh, loadData]);

  const handleSimulateSale = async (product, qty = 1) => {
    try {
      await simulateOrder(product.id, qty);
      showToast(`Sale recorded: ${qty}x ${product.name}. Stock decreased, demand velocity updated!`, 'success');
      await loadData(true);
    } catch (err) {
      showToast(`Order failed: ${err.message}`, 'error');
    }
  };

  const handleUpdateStock = async (product, newStock) => {
    if (newStock < 0) return;
    try {
      await updateStock(product.id, newStock);
      showToast(`Stock updated to ${newStock} for ${product.name}`, 'success');
      await loadData(true);
    } catch (err) {
      showToast(`Stock update failed: ${err.message}`, 'error');
    }
  };

  const handleRequestPricing = async (product) => {
    try {
      const res = await requestPricingSuggestion(product.id);
      showToast(`Generated on-demand pricing recommendation for ${product.name}: $${res.recommendedPrice}`, 'success');
      await loadData(true);
    } catch (err) {
      showToast(`Pricing suggestion error: ${err.message}`, 'error');
    }
  };

  const handleRequestReorder = async (product) => {
    try {
      const res = await requestReorderSuggestion(product.id);
      showToast(`Generated on-demand reorder recommendation for ${product.name}: ${res.recommendedQuantity} units`, 'success');
      await loadData(true);
    } catch (err) {
      showToast(`Reorder suggestion error: ${err.message}`, 'error');
    }
  };

  const handleDecidePricing = async (suggestionId, decision) => {
    try {
      const res = await decidePricingSuggestion(suggestionId, decision);
      if (decision === 'ACCEPTED') {
        showToast(`Pricing recommendation accepted! Live price updated to $${res.recommendedPrice.toFixed(2)}`, 'success');
      } else {
        showToast('Pricing recommendation rejected. Catalog price left unchanged.', 'info');
      }
      await loadData(true);
    } catch (err) {
      showToast(`Decision failed: ${err.message}`, 'error');
    }
  };

  const handleDecideReorder = async (suggestionId, decision) => {
    try {
      const res = await decideReorderSuggestion(suggestionId, decision);
      if (decision === 'ACCEPTED') {
        showToast(`Reorder recommendation accepted! Inbound replenishment applied (+${res.recommendedQuantity} units).`, 'success');
      } else {
        showToast('Reorder recommendation rejected.', 'info');
      }
      await loadData(true);
    } catch (err) {
      showToast(`Decision failed: ${err.message}`, 'error');
    }
  };

  const handleStrategyChange = async (newStrat) => {
    try {
      const res = await switchStrategy(newStrat);
      setActiveStrategy(res.activeStrategy);
      showToast(`Active Commerce Engine switched to ${res.activeStrategy} without restart!`, 'success');
    } catch (err) {
      showToast(`Failed to switch strategy: ${err.message}`, 'error');
    }
  };

  const totalPending = pricingSuggestions.length + reorderSuggestions.length;

  return (
    <div className="app-container">
      {/* Header */}
      <header className="header">
        <div className="header-brand">
          <div className="header-badge">AI COMMERCE ENGINE · REALTIME</div>
          <h1 className="header-title">Stock<em>Pulse</em></h1>
          <div className="header-subtitle">Autonomous Inventory Signal Detection & Dynamic Pricing Loop</div>
        </div>

        <div className="header-controls">
          <div className="strategy-selector-box">
            <span className="control-label">Active Strategy:</span>
            <div className="strategy-toggle-group">
              {availableStrategies.map((s) => (
                <button
                  key={s}
                  type="button"
                  className={`strategy-btn ${activeStrategy === s ? 'active' : ''}`}
                  onClick={() => handleStrategyChange(s)}
                >
                  {s === 'RULE_BASED' ? '⚡ Rule-Based' : '✨ AI Advisor'}
                </button>
              ))}
            </div>
          </div>

          <div className="refresh-controls">
            <button
              type="button"
              className={`pill-btn ${autoRefresh ? 'active-poll' : ''}`}
              onClick={() => setAutoRefresh(!autoRefresh)}
              title="Toggle 3s live polling"
            >
              <span className="poll-indicator"></span>
              {autoRefresh ? 'Live Polling ON' : 'Polling Paused'}
            </button>
            <button type="button" className="action-btn-sm" onClick={() => loadData(false)}>
              ↻ Refresh
            </button>
          </div>
        </div>
      </header>

      {/* Toast banner */}
      {toast && (
        <div className={`toast-banner toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button type="button" className="toast-close" onClick={() => setToast(null)}>×</button>
        </div>
      )}

      {error && (
        <div className="error-banner">
          <strong>Backend Connection Notice:</strong> {error}
        </div>
      )}

      {/* Loop Explanation Bar */}
      <div className="loop-status-bar">
        <div className="loop-step done">
          <span className="step-num">1</span>
          <span className="step-text"><strong>Signal:</strong> Sale / Stock Update</span>
        </div>
        <div className="loop-arrow">→</div>
        <div className="loop-step active">
          <span className="step-num">2</span>
          <span className="step-text"><strong>Detect:</strong> Low Stock or Demand Spike</span>
        </div>
        <div className="loop-arrow">→</div>
        <div className="loop-step">
          <span className="step-num">3</span>
          <span className="step-text"><strong>Recommend:</strong> {activeStrategy} Strategy</span>
        </div>
        <div className="loop-arrow">→</div>
        <div className="loop-step highlight">
          <span className="step-num">4</span>
          <span className="step-text"><strong>Approve:</strong> {totalPending} Pending Approvals</span>
        </div>
        <div className="loop-arrow">→</div>
        <div className="loop-step">
          <span className="step-num">5</span>
          <span className="step-text"><strong>Act:</strong> Live Price & Stock Updated</span>
        </div>
      </div>

      {/* Pending Suggestions Section */}
      <section className="section-container">
        <div className="section-header">
          <div>
            <h2 className="section-title">
              Pending Merchandising Recommendations
              {totalPending > 0 && <span className="count-pill">{totalPending}</span>}
            </h2>
            <p className="section-desc">
              AI & Rule proposals generated automatically by inventory and velocity triggers. Merchandising human approval required before changes take effect.
            </p>
          </div>
        </div>

        {totalPending === 0 ? (
          <div className="empty-state-card">
            <div className="empty-icon">✓</div>
            <h3>All recommendations reviewed</h3>
            <p>No suggestions pending. Simulate a sale on <strong>Organic Cotton T-Shirt</strong> (PRD-003) to trigger low inventory, or multiple sales on <strong>Hoodie</strong> (PRD-008) to trigger a demand spike!</p>
          </div>
        ) : (
          <div className="suggestions-grid">
            {/* Pricing Suggestions */}
            {pricingSuggestions.map((s) => (
              <div key={`pricing-${s.id}`} className="suggestion-card pricing-card">
                <div className="card-top">
                  <span className="type-badge pricing">PRICE ADJUSTMENT</span>
                  <span className={`trigger-badge trigger-${s.triggerReason.toLowerCase()}`}>
                    {s.triggerReason.replace('_', ' ')}
                  </span>
                  <span className="strategy-tag">{s.strategy || 'STRATEGY'}</span>
                </div>

                <div className="card-product-info">
                  <h3 className="product-title">{s.productName}</h3>
                  <span className="sku-tag">{s.productSku}</span>
                </div>

                <div className="card-metrics-row">
                  <div className="metric-box">
                    <span className="metric-label">Current Price</span>
                    <span className="metric-val strike">${s.currentPrice.toFixed(2)}</span>
                  </div>
                  <div className="metric-arrow">→</div>
                  <div className="metric-box target">
                    <span className="metric-label">Recommended Price</span>
                    <span className="metric-val highlight">${s.recommendedPrice.toFixed(2)}</span>
                  </div>
                  <div className="metric-box confidence">
                    <span className="metric-label">Confidence</span>
                    <span className="metric-val">{(s.confidence * 100).toFixed(0)}%</span>
                  </div>
                </div>

                <div className="card-reasoning">
                  <strong>Rationale:</strong> {s.reasoning}
                </div>

                <div className="card-actions">
                  <button
                    type="button"
                    className="btn btn-accept"
                    onClick={() => handleDecidePricing(s.id, 'ACCEPTED')}
                  >
                    ✓ Approve Price Change
                  </button>
                  <button
                    type="button"
                    className="btn btn-reject"
                    onClick={() => handleDecidePricing(s.id, 'REJECTED')}
                  >
                    ✕ Reject
                  </button>
                </div>
              </div>
            ))}

            {/* Reorder Suggestions */}
            {reorderSuggestions.map((s) => (
              <div key={`reorder-${s.id}`} className="suggestion-card reorder-card">
                <div className="card-top">
                  <span className="type-badge reorder">REORDER / REPLENISHMENT</span>
                  <span className={`trigger-badge trigger-${s.triggerReason.toLowerCase()}`}>
                    {s.triggerReason.replace('_', ' ')}
                  </span>
                  <span className="strategy-tag">{s.strategy || 'STRATEGY'}</span>
                </div>

                <div className="card-product-info">
                  <h3 className="product-title">{s.productName}</h3>
                  <span className="sku-tag">{s.productSku}</span>
                </div>

                <div className="card-metrics-row">
                  <div className="metric-box">
                    <span className="metric-label">Current Stock</span>
                    <span className="metric-val">{s.currentStock} units</span>
                  </div>
                  <div className="metric-arrow">+</div>
                  <div className="metric-box target">
                    <span className="metric-label">Reorder Quantity</span>
                    <span className="metric-val highlight">+{s.recommendedQuantity} units</span>
                  </div>
                  <div className="metric-box">
                    <span className="metric-label">Lead Time</span>
                    <span className="metric-val">{s.suggestedLeadTimeDays} days</span>
                  </div>
                  <div className="metric-box confidence">
                    <span className="metric-label">Confidence</span>
                    <span className="metric-val">{(s.confidence * 100).toFixed(0)}%</span>
                  </div>
                </div>

                <div className="card-reasoning">
                  <strong>Rationale:</strong> {s.reasoning}
                </div>

                <div className="card-actions">
                  <button
                    type="button"
                    className="btn btn-accept"
                    onClick={() => handleDecideReorder(s.id, 'ACCEPTED')}
                  >
                    ✓ Approve Reorder
                  </button>
                  <button
                    type="button"
                    className="btn btn-reject"
                    onClick={() => handleDecideReorder(s.id, 'REJECTED')}
                  >
                    ✕ Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Catalog & Simulation Workbench */}
      <section className="section-container">
        <div className="section-header">
          <div>
            <h2 className="section-title">ShopStream Product Catalog & Simulation Workbench</h2>
            <p className="section-desc">
              Live inventory tracking and instant order simulation to trigger autonomous merchandising recommendations.
            </p>
          </div>

          <div className="catalog-filters">
            <div className="filter-item">
              <label>Category:</label>
              <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
                <option value="ALL">All Categories</option>
                <option value="ELECTRONICS">Electronics</option>
                <option value="APPAREL">Apparel</option>
                <option value="HOME">Home</option>
              </select>
            </div>
            <div className="filter-item">
              <label>Status:</label>
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="PRICE_REVIEW_PENDING">Review Pending</option>
                <option value="OUT_OF_STOCK">Out of Stock</option>
              </select>
            </div>
          </div>
        </div>

        {loading && products.length === 0 ? (
          <div className="loading-state">Loading product catalog...</div>
        ) : (
          <div className="table-responsive">
            <table className="catalog-table">
              <thead>
                <tr>
                  <th>Product / SKU</th>
                  <th>Category</th>
                  <th>Current Price</th>
                  <th>Stock vs Threshold</th>
                  <th>24h Velocity</th>
                  <th>Lifecycle Status</th>
                  <th>Simulate Sale (Agentic Trigger)</th>
                  <th>Manual Advice</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => {
                  const isLow = p.stockLevel < p.reorderThreshold && p.stockLevel > 0;
                  const isOut = p.stockLevel === 0;
                  return (
                    <tr key={p.id} className={isLow ? 'row-low-stock' : isOut ? 'row-out-stock' : ''}>
                      <td>
                        <div className="tbl-product-name">{p.name}</div>
                        <div className="tbl-sku">{p.sku}</div>
                      </td>
                      <td>
                        <span className={`cat-pill cat-${p.category.toLowerCase()}`}>{p.category}</span>
                      </td>
                      <td>
                        <span className="tbl-price">${p.currentPrice.toFixed(2)}</span>
                      </td>
                      <td>
                        <div className="stock-cell">
                          <span className={`stock-number ${isLow ? 'stock-low' : isOut ? 'stock-zero' : 'stock-ok'}`}>
                            {p.stockLevel}
                          </span>
                          <span className="stock-threshold">/ {p.reorderThreshold} target</span>
                          {isLow && <span className="warning-pill">LOW</span>}
                          {isOut && <span className="danger-pill">OUT</span>}
                        </div>
                        <div className="stock-bar-bg">
                          <div
                            className={`stock-bar-fill ${isLow ? 'fill-low' : isOut ? 'fill-zero' : 'fill-ok'}`}
                            style={{ width: `${Math.min(100, (p.stockLevel / (p.reorderThreshold * 2)) * 100)}%` }}
                          ></div>
                        </div>
                      </td>
                      <td>
                        <div className="velocity-cell">
                          <span className="velocity-val">{p.demandVelocity} orders</span>
                          {p.demandVelocity >= 10 && <span className="spike-pill">🔥 SURGE</span>}
                        </div>
                      </td>
                      <td>
                        <span className={`status-badge status-${p.status.toLowerCase()}`}>
                          {p.status.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td>
                        <div className="btn-group">
                          <button
                            type="button"
                            className="sim-btn"
                            onClick={() => handleSimulateSale(p, 1)}
                            title="Simulate 1 sale"
                          >
                            ⚡ Order 1
                          </button>
                          <button
                            type="button"
                            className="sim-btn sim-btn-multi"
                            onClick={() => handleSimulateSale(p, 5)}
                            title="Simulate 5 sales (viral burst)"
                          >
                            ⚡⚡ Order 5
                          </button>
                        </div>
                      </td>
                      <td>
                        <div className="btn-group">
                          <button
                            type="button"
                            className="btn-advice"
                            onClick={() => handleRequestPricing(p)}
                            title="Generate on-demand pricing recommendation"
                          >
                            💡 Price
                          </button>
                          <button
                            type="button"
                            className="btn-advice"
                            onClick={() => handleRequestReorder(p)}
                            title="Generate on-demand reorder recommendation"
                          >
                            📦 Reorder
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="footer-content">
          <span>StockPulse AI Engine · Built for ShopStream Merchandising Operations</span>
          <span>Last Poll: {lastRefreshed.toLocaleTimeString()}</span>
        </div>
      </footer>
    </div>
  );
}
