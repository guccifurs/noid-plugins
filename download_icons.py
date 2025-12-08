
import os
import urllib.request
import urllib.parse

prayers = [
    "Thick Skin", "Burst of Strength", "Clarity of Thought", "Sharp Eye", "Mystic Will",
    "Rock Skin", "Superhuman Strength", "Improved Reflexes", "Rapid Restore", "Rapid Heal",
    "Protect Item", "Hawk Eye", "Mystic Lore", "Steel Skin", "Ultimate Strength",
    "Incredible Reflexes", "Protect from Magic", "Protect from Missiles", "Protect from Melee",
    "Eagle Eye", "Mystic Might", "Retribution", "Redemption", "Smite",
    "Preserve", "Chivalry", "Piety", "Rigour", "Augury"
]

base_url = "https://oldschool.runescape.wiki/images/"
output_dir = "downloaded_icons"

if not os.path.exists(output_dir):
    os.makedirs(output_dir)

for prayer in prayers:
    # Mimic the Java logic: replace space with _, ' with %27
    formatted = prayer.strip().replace(" ", "_").replace("'", "%27")
    filename = formatted + ".png"
    url = base_url + filename
    
    # Final filename on disk (keep it simple, lowercase with underscores)
    save_name = prayer.lower().replace(" ", "_").replace("'", "") + ".png"
    save_path = os.path.join(output_dir, save_name)
    
    print(f"Downloading {prayer} from {url} to {save_path}...")
    
    try:
        # User-Agent is often required for wiki
        req = urllib.request.Request(
            url, 
            data=None, 
            headers={
                'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        )
        with urllib.request.urlopen(req) as response, open(save_path, 'wb') as out_file:
            out_file.write(response.read())
        print("Success.")
    except Exception as e:
        print(f"Failed: {e}")

