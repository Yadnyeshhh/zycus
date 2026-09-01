const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export async function fetchProducts(status, category) {
  const params = new URLSearchParams();
  if (status) params.append('status', status);
  if (category) params.append('category', category);
  const url = `${API_BASE}/products${params.toString() ? '?' + params.toString() : ''}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.statusText}`);
  return res.json();
}

export async function simulateOrder(productId, quantity = 1) {
  const res = await fetch(`${API_BASE}/products/${productId}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity })
  });
  if (!res.ok) throw new Error(`Failed to simulate order: ${res.statusText}`);
  return res.json();
}

export async function updateStock(productId, stockLevel) {
  const res = await fetch(`${API_BASE}/products/${productId}/stock`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ stockLevel })
  });
  if (!res.ok) throw new Error(`Failed to update stock: ${res.statusText}`);
  return res.json();
}

export async function requestPricingSuggestion(productId) {
  const res = await fetch(`${API_BASE}/products/${productId}/suggest-pricing`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Failed to request pricing suggestion: ${res.statusText}`);
  return res.json();
}

export async function requestReorderSuggestion(productId) {
  const res = await fetch(`${API_BASE}/products/${productId}/suggest-reorder`, {
    method: 'POST'
  });
  if (!res.ok) throw new Error(`Failed to request reorder suggestion: ${res.statusText}`);
  return res.json();
}

export async function fetchPricingSuggestions(status = 'PENDING') {
  const url = status ? `${API_BASE}/pricing-suggestions?status=${status}` : `${API_BASE}/pricing-suggestions`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch pricing suggestions: ${res.statusText}`);
  return res.json();
}

export async function fetchReorderSuggestions(status = 'PENDING') {
  const url = status ? `${API_BASE}/reorder-suggestions?status=${status}` : `${API_BASE}/reorder-suggestions`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to fetch reorder suggestions: ${res.statusText}`);
  return res.json();
}

export async function decidePricingSuggestion(id, decision) {
  const res = await fetch(`${API_BASE}/pricing-suggestions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: decision })
  });
  if (!res.ok) throw new Error(`Failed to ${decision} pricing suggestion: ${res.statusText}`);
  return res.json();
}

export async function decideReorderSuggestion(id, decision) {
  const res = await fetch(`${API_BASE}/reorder-suggestions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: decision })
  });
  if (!res.ok) throw new Error(`Failed to ${decision} reorder suggestion: ${res.statusText}`);
  return res.json();
}

export async function fetchActiveStrategy() {
  const res = await fetch(`${API_BASE}/commerce/strategy`);
  if (!res.ok) throw new Error(`Failed to fetch active strategy: ${res.statusText}`);
  return res.json();
}

export async function switchStrategy(strategy) {
  const res = await fetch(`${API_BASE}/commerce/strategy`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ strategy })
  });
  if (!res.ok) throw new Error(`Failed to switch strategy: ${res.statusText}`);
  return res.json();
}
