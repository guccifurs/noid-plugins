const https = require('https');

const PLISIO_API_KEY = process.env.PLISIO_API_KEY;
const PLISIO_BASE_URL = 'https://plisio.net/api/v1';

/**
 * Create a Plisio invoice for crypto payment
 * @param {Object} params
 * @param {string} params.orderId - Unique order ID (Discord user ID)
 * @param {number} params.amount - Amount in USD
 * @param {string} params.orderName - Description of order
 * @returns {Promise<Object>} Invoice data with payment address and QR code
 */
function createInvoice({ orderId, amount, orderName = 'GP Deposit' }) {
  return new Promise((resolve, reject) => {
    if (!PLISIO_API_KEY) {
      return reject(new Error('PLISIO_API_KEY not configured'));
    }

    // Make order_number unique by adding timestamp
    const uniqueOrderId = `${orderId}-${Date.now()}`;

    const params = new URLSearchParams({
      api_key: PLISIO_API_KEY,
      order_number: uniqueOrderId,
      order_name: orderName,
      source_currency: 'USD',
      source_amount: amount.toFixed(2),
      currency: 'USDT', // Accept USDT by default (can be changed to BTC, ETH, etc.)
      callback_url: 'none', // We'll use polling instead of callbacks
    });

    const options = {
      hostname: 'plisio.net',
      path: `/api/v1/invoices/new?${params.toString()}`,
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.status === 'error') {
            return reject(new Error(json.data?.message || 'Plisio API error'));
          }
          if (json.status === 'success' && json.data) {
            resolve(json.data);
          } else {
            reject(new Error('Invalid Plisio response'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', err => reject(err));
    req.end();
  });
}

/**
 * Get full invoice details including payment address and QR code
 * @param {string} txnId - Transaction ID from Plisio
 * @returns {Promise<Object>} Full invoice details
 */
function getInvoiceDetails(txnId) {
  return new Promise((resolve, reject) => {
    if (!PLISIO_API_KEY) {
      return reject(new Error('PLISIO_API_KEY not configured'));
    }

    const params = new URLSearchParams({
      api_key: PLISIO_API_KEY,
    });

    const options = {
      hostname: 'plisio.net',
      path: `/api/v1/operations/${txnId}?${params.toString()}`,
      method: 'GET',
    };

    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.status === 'error') {
            return reject(new Error(json.data?.message || 'Plisio API error'));
          }
          if (json.status === 'success' && json.data) {
            resolve(json.data);
          } else {
            reject(new Error('Invalid Plisio response'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', err => reject(err));
    req.end();
  });
}

/**
 * Check the status of a Plisio invoice
 * @param {string} txnId - Transaction ID from Plisio
 * @returns {Promise<Object>} Invoice status
 */
function checkInvoiceStatus(txnId) {
  return new Promise((resolve, reject) => {
    if (!PLISIO_API_KEY) {
      return reject(new Error('PLISIO_API_KEY not configured'));
    }

    const params = new URLSearchParams({
      api_key: PLISIO_API_KEY,
      txn_id: txnId,
    });

    const options = {
      hostname: 'plisio.net',
      path: `/api/v1/operations/${txnId}?${params.toString()}`,
      method: 'GET',
    };

    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.status === 'error') {
            return reject(new Error(json.data?.message || 'Plisio API error'));
          }
          if (json.status === 'success' && json.data) {
            resolve(json.data);
          } else {
            reject(new Error('Invalid Plisio response'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', err => reject(err));
    req.end();
  });
}

/**
 * Create a mass payout (withdrawal) to send crypto to user
 * @param {Object} params
 * @param {string} params.currency - Crypto currency (usdt, btc, eth)
 * @param {string} params.address - User's wallet address
 * @param {number} params.amount - Amount to send
 * @param {string} params.type - 'cash_out' for withdrawals
 * @returns {Promise<Object>} Payout data
 */
function createPayout({ currency, address, amount, type = 'cash_out' }) {
  return new Promise((resolve, reject) => {
    if (!PLISIO_API_KEY) {
      return reject(new Error('PLISIO_API_KEY not configured'));
    }

    const params = new URLSearchParams({
      api_key: PLISIO_API_KEY,
      currency: currency.toUpperCase(),  // Plisio uses uppercase: BTC, USDT, LTC
      to: address,  // Plisio uses 'to' not 'to_address'
      amount: amount.toString(),
      type: type,
    });
    
    // Debug logging
    console.log('Plisio payout request:', {
      currency: currency.toUpperCase(),
      address: address.substring(0, 10) + '...',
      amount: amount.toString(),
      type
    });

    const options = {
      hostname: 'plisio.net',
      path: `/api/v1/operations/withdraw?${params.toString()}`,
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    };

    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.status === 'error') {
            return reject(new Error(json.data?.message || 'Plisio payout error'));
          }
          if (json.status === 'success' && json.data) {
            resolve(json.data);
          } else {
            reject(new Error('Invalid Plisio payout response'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', err => reject(err));
    req.end();
  });
}

/**
 * Get Plisio balance for a specific currency
 * @param {string} currency - Currency code (usdt, btc, eth, etc.)
 * @returns {Promise<Object>} Balance data
 */
function getBalance(currency) {
  return new Promise((resolve, reject) => {
    if (!PLISIO_API_KEY) {
      return reject(new Error('PLISIO_API_KEY not configured'));
    }

    const params = new URLSearchParams({
      api_key: PLISIO_API_KEY,
      currency: currency.toLowerCase(),
    });

    const options = {
      hostname: 'plisio.net',
      path: `/api/v1/balances/${currency.toLowerCase()}?${params.toString()}`,
      method: 'GET',
    };

    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => {
        data += chunk;
      });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.status === 'error') {
            return reject(new Error(json.data?.message || 'Plisio balance error'));
          }
          if (json.status === 'success' && json.data) {
            resolve(json.data);
          } else {
            reject(new Error('Invalid Plisio balance response'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', err => reject(err));
    req.end();
  });
}

module.exports = {
  createInvoice,
  getInvoiceDetails,
  checkInvoiceStatus,
  createPayout,
  getBalance,
};
