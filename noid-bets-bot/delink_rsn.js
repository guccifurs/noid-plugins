const Database = require('better-sqlite3');
const path = require('path');
const DB_PATH = path.join(__dirname, '..', 'users', 'noidbets.db');
const db = new Database(DB_PATH);
console.log(`Using database: ${DB_PATH}\n`);

// First check what tables exist
console.log('Checking database tables...');
const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table'").all();
console.log('Tables:', tables.map(t => t.name));

// Check if user_rsns table exists
const hasUserRsns = tables.some(t => t.name === 'user_rsns');

if (!hasUserRsns) {
  console.log('\n❌ user_rsns table does not exist!');
  console.log('Creating user_rsns table...');
  
  db.exec(`
    CREATE TABLE IF NOT EXISTS user_rsns (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      rsn TEXT NOT NULL,
      linked_at INTEGER DEFAULT (strftime('%s', 'now')),
      UNIQUE(user_id, rsn)
    );
    CREATE INDEX IF NOT EXISTS idx_rsn ON user_rsns(rsn);
  `);
  
  console.log('✓ user_rsns table created');
}

// Show all RSNs with detailed info
const all = db.prepare('SELECT rsn, user_id FROM user_rsns').all();
console.log(`\nAll linked RSNs (${all.length}):`);
all.forEach(r => {
  console.log(`  - RSN: "${r.rsn}"`);
  console.log(`    User ID: ${r.user_id}`);
  console.log(`    Length: ${r.rsn.length}`);
  console.log(`    Bytes: ${Buffer.from(r.rsn).toString('hex')}`);
  console.log(`    Trimmed: "${r.rsn.trim()}"`);
  console.log(`    Lower: "${r.rsn.toLowerCase()}"`);
  console.log('');
});

// Test lookups
console.log('Testing lookups:');
const testRsns = ['SG Nh-mation', 'sg nh-mation', 'SG  Nh-mation'];
testRsns.forEach(testRsn => {
  const result = db.prepare(`
    SELECT u.* FROM users u
    JOIN user_rsns ur ON ur.user_id = u.id
    WHERE LOWER(ur.rsn) = LOWER(?)
  `).get(testRsn);
  
  console.log(`  Lookup "${testRsn}": ${result ? 'FOUND (user ' + result.id + ')' : 'NOT FOUND'}`);
});

db.close();
