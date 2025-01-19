import json
import os
import time
import urllib.request

json_file = "trackers.json"
output_file = "trackers.xml"

# Download the JSON file from Exodus: https://reports.exodus-privacy.eu.org/api/trackers
# and save it in the same folder as this script and show download progress
decision = input("Do you want to download the JSON file from Exodus? (y/n): ")
startTimeMillis = time.time()

if decision.lower() == "y":
    # Delete the existing JSON file
    try:
        os.remove(json_file)
    except FileNotFoundError:
        pass

    print("Downloading", json_file, "from Exodus...")
    with urllib.request.urlopen("https://reports.exodus-privacy.eu.org/api/trackers") as response:
        with open(json_file, "wb") as file:
            file.write(response.read())
            file.close()
            print("Downloaded", json_file)

# Format the JSON file
print("Formatting", json_file, "...")
with open(json_file, "r+", encoding="utf8") as file:
    json_content = json.load(file)
    json_content = json.dumps(json_content, indent=4, sort_keys=True)
    file.seek(0)
    file.write(json_content)

# If file is not found, create it
# else delete it and create a new one
try:
    open(output_file, "r")
    print("File already exists. Deleting and creating a new one...")
except FileNotFoundError:
    open(output_file, "w").close()
    print("File not found. Creating a new one...")
else:
    os.remove(output_file)
    open(output_file, "w").close()
    print("File deleted. Created a new one...")

# Read the JSON file
with open(json_file, "r", encoding="utf8") as file:
    json_content = json.load(file)

# Sort the JSON file by code_signature
json_content["trackers"] = \
    dict(sorted(json_content["trackers"].items(), key=lambda x: x[1]["code_signature"]))

# Extract the trackers tags

# Get the signature from the JSON file
# also check if some signatures are missing
# or have more than one signature separated
# by a |
signatures = []

for tracker in json_content["trackers"].values():
    try:
        if tracker["code_signature"].find("|") != -1:
            for signature in tracker["code_signature"].split("|"):
                signatures.append(signature)

            print("Multiple signatures found for", tracker["website"])
        else:
            signatures.append(tracker["code_signature"])
    except KeyError:
        print("Signature not found for", tracker["website"])


websites = [tracker["website"] for tracker in json_content["trackers"].values()]
names = [tracker["name"] for tracker in json_content["trackers"].values()]
print("Total signatures:", len(signatures))
print("Total websites:", len(websites))
print("Total names:", len(names))

# Format the output
output = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n"
output += "<!--suppress CheckTagEmptyBody -->\n"
output += '<resources>\n\n'
output += "\t<!-- This file is auto-generated by the parser.py script\n"
output += "\t     located in the trackers folder. -->\n\n"
output += "\t<!-- Total trackers: " + str(len(signatures)) + " -->\n\n"
output += '\t<string-array name="trackers">\n'
output += "\n".join([f'\t\t<item>{signature}</item> <!-- '
                     f'{signatures.index(signature) + 1} -->' for signature in signatures])
output += "\n\t</string-array>"

output += "\n\n<!-- Total websites: " + str(len(websites)) + " -->\n\n"
output += '\t<string-array name="websites">\n'
output += "\n".join([f'\t\t<item>{website}</item> <!-- '
                     f'{websites.index(website) + 1} -->' for website in websites])
output += "\n\t</string-array>"

output += "\n\n<!-- Total names: " + str(len(names)) + " -->\n\n"
output += '\t<string-array name="names">\n'
output += "\n".join([f'\t\t<item>{name}</item> <!-- '
                     f'{names.index(name) + 1} -->' for name in names])
output += "\n\t</string-array>"
output += "\n\n</resources>"

# Write the output to the text file
with open(output_file, "w") as file:
    file.write(output)

print("Output written to", output_file)

# Copy the file to \Inure\app\src\main\res\values\trackers.xml
# The path Inure is one level above the trackers folder

# Check if the file exists
decision = input("Do you want to copy the file to Inure? (y/n): ")
if decision.lower() == "y":
    try:
        open(f"..\\..\\app\\src\\main\\res\\values\\{output_file}", "r")
        print("File already exists at: ..\\..\\app\\src\\main\\res\\values\\trackers.xml")
        print("Deleting..")
        os.remove(f"..\\..\\app\\src\\main\\res\\values\\{output_file}")
    except FileNotFoundError:
        print("File not found at: ..\\..\\app\\src\\main\\res\\values\\trackers.xml")
    finally:
        os.system(f"copy {output_file} ..\\..\\app\\src\\main\\res\\values\\trackers.xml")
        print("Copied to: ..\\..\\app\\src\\main\\res\\values\\trackers.xml")

# Copy trackers JSON to main/resources dir
# Check if the file exists
decision = input("Do you want to copy the JSON file to Inure? (y/n): ")
if decision.lower() == "y":
    try:
        open(f"..\\..\\app\\src\\main\\resources\\{json_file}", "r")
        print("File already exists at: ..\\..\\app\\src\\main\\resources\\trackers.json")
        print("Deleting..")
        os.remove(f"..\\..\\app\\src\\main\\resources\\{json_file}")
    except FileNotFoundError:
        print("File not found at: ..\\..\\app\\src\\main\\resources\\trackers.json")
    finally:
        os.system(f"copy {json_file} ..\\..\\app\\src\\main\\resources\\trackers.json")
        print("Copied to: ..\\..\\app\\src\\main\\resources\\trackers.json")

print("Total time taken:", round(time.time() - startTimeMillis, 2), "seconds")

# Open the output file
os.startfile(output_file)
