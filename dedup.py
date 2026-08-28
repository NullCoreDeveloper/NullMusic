import os
import re
replacements = {
    "applicationId = \"iad1tya.echo.music\"": "applicationId = \"com.nullcore.music\"",
    "https://t.me/EchoMusicApp": "https://t.me/NullCoreDeveloper",
    "iad1tya.cyou": "NullCoreDeveloper",
    "https://instagram.com/iad1tya": "https://t.me/NullCoreDeveloper",
    "buymeacoffee.com/iad1tya": "github.com/NullCoreDeveloper",
    "patreon.com/cw/iad1tya": "t.me/NullCoreDeveloper",
    "iad1tya@upi": "nullcore@upi",
    "iad1tya/Echo-Music": "NullCoreDeveloper/NullMusic",
    "EchoMusicApp/Echo-Music": "NullCoreDeveloper/NullMusic",
    "Echo Music": "NullMusic",
    "echomusic": "nullmusic"
}
for root, dirs, files in os.walk("."):
    if ".git" in root or "build" in root or ".gradle" in root:
        continue
    for file in files:
        if file.endswith((".kt", ".md", ".json", ".xml", ".kts", ".properties")):
            path = os.path.join(root, file)
            try:
                with open(path, "r", encoding="utf-8") as f: content = f.read()
                new_content = content
                for old, new in replacements.items(): new_content = new_content.replace(old, new)
                
                # Automatically strip duplicate string keys in Android XML resources
                if path.endswith(".xml") and "values" in path:
                    keys_seen = set()
                    lines = new_content.splitlines()
                    dedup_lines = []
                    for line in lines:
                        match = re.search(r"<string name=\"([^\"]+)\"", line)
                        if match:
                            key = match.group(1)
                            if key in keys_seen:
                                continue
                            keys_seen.add(key)
                        dedup_lines.append(line)
                    if lines:
                        new_content = "\n".join(dedup_lines) + "\n"
                
                if new_content != content:
                    with open(path, "w", encoding="utf-8") as f: f.write(new_content)
            except Exception: pass
