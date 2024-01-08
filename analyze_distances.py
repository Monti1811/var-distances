import json
import io
import os

BASE_PATH_TO_REPOS = 'F:\\HiWi\\java_github_repos\\'

GITHUB_BASE_ADDRESS = "https://github.com/"
GITHUB_END_ADDRESS = ".git"

repos = []
repos_json = json.load(io.open('results_best_repos.json', 'r'))
for item in repos_json["items"]:
    repo = item["name"]
    # Get the name of the repo from the name in the json (name is in the format "user/repo")
    name = repo.split("/")[1]
    repos.append(name)
    # Check if a folder exists with the name of the repo
    if not os.path.exists(BASE_PATH_TO_REPOS + name):
        # If not, clone the repo
        os.system("git clone " + GITHUB_BASE_ADDRESS + repo + GITHUB_END_ADDRESS + " " + BASE_PATH_TO_REPOS + name)


json_results = {}
try:
    json_results = json.load(io.open('results.json', 'r'))
except:
    print("No results.json file found")


for repo in repos:
    # Run distancecalculator.jar for each repo
    if not repo in json_results:
        print("Running distancecalculator.jar for " + repo)
        os.system("\"C:\\Program Files\\Java\\jdk-17.0.4.1\\bin\\java.exe\" -jar out/artifacts/distancecalculator_jar/distancecalculator.jar 1 " + BASE_PATH_TO_REPOS + repo + " " + repo)

# Set the total results
json_results = json.load(io.open('results.json', 'r'))
json_results.pop("total")
total_results = {}
total_declarations = 0
total_distance = 0
for repo in json_results:
    # Add the results of the repo to the total results
    for key, value in json_results[repo]["distances"].items():
        if key not in total_results:
            total_results[key] = value
        else:
            total_results[key] += value
    total_declarations += json_results[repo]["values"]["totalDeclarations"]
    total_distance += json_results[repo]["values"]["totalDistance"]

# Sort the total_results by the key (distance), which is a string
total_results = dict(sorted(total_results.items(), key=lambda item: float(item[0])))

json_results["total"] = {}
json_results["total"]["distances"] = total_results
json_results["total"]["values"] = {}
json_results["total"]["values"]["totalDeclarations"] = total_declarations
json_results["total"]["values"]["totalDistance"] = total_distance
json_results["total"]["values"]["averageDistance"] = total_distance / total_declarations
# Median distance
counter = 0
for key, value in total_results.items():
    counter += value
    if counter > total_declarations / 2:
        json_results["total"]["values"]["medianDistance"] = float(key)
        break
# Standard error of average distance
json_results["total"]["values"]["standardError"] = (total_distance / total_declarations) / (total_declarations ** 0.5)

# Write it to the json file
with io.open('results.json', 'w', encoding='utf-8') as f:
    f.write(json.dumps(json_results, indent=4, ensure_ascii=False))
