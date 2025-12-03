const Database = require('better-sqlite3');
const path = require('path');
const DB_PATH = path.join(__dirname, '..', 'users', 'noidbets.db');
const db = new Database(DB_PATH);

console.log(`Using database: ${DB_PATH}\n`);

// Get all RSNs
const allRsns = db.prepare('SELECT rsn, user_id FROM user_rsns').all();

console.log('Checking for RSNs with non-breaking spaces...\n');

let fixed = 0;
allRsns.forEach(row => {
  const originalRsn = row.rsn;
  const normalizedRsn = originalRsn.replace(/\u00A0/g, ' ');
  
  if (originalRsn !== normalizedRsn) {
    console.log(`Found problematic RSN:`);
    console.log(`  Original: "${originalRsn}"`);
    console.log(`  Original bytes: ${Buffer.from(originalRsn).toString('hex')}`);
    console.log(`  Normalized: "${normalizedRsn}"`);
    console.log(`  Normalized bytes: ${Buffer.from(normalizedRsn).toString('hex')}`);
    console.log(`  User ID: ${row.user_id}`);
    
    // Update the RSN
    try {
      db.prepare('UPDATE user_rsns SET rsn = ? WHERE user_id = ? AND rsn = ?')
        .run(normalizedRsn, row.user_id, originalRsn);
      
      console.log(`  ✓ FIXED!\n`);
      fixed++;
    } catch (err) {
      console.log(`  ✗ Error: ${err.message}\n`);
    }
  }
});

if (fixed === 0) {
  console.log('No RSNs with non-breaking spaces found. All good! ✓');
} else {
  console.log(`\nFixed ${fixed} RSN(s) with non-breaking spaces.`);
}

// Show all RSNs after fix
console.log('\nAll RSNs after fix:');
const updatedRsns = db.prepare('SELECT rsn, user_id FROM user_rsns').all();
updatedRsns.forEach(r => {
  console.log(`  - "${r.rsn}" (user: ${r.user_id})`);
  console.log(`    Bytes: ${Buffer.from(r.rsn).toString('hex')}`);
});

db.close();
