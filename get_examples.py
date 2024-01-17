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


repo = "elasticsearch/server/src/main"

# Structure of find_examples.json:
# key = distance
# value = array of size 4
# [0] amount of times that are ignored before saving examples,
# [1] amount of examples to save,
# [2] amount of times that are ignored before saving double declaration examples,
# [3] amount of double declaration examples to save

os.system("\"C:\\Program Files\\Java\\jdk-17.0.4.1\\bin\\java.exe\" -jar distancecalculator.jar ex " + BASE_PATH_TO_REPOS + repo + " find_examples.json")

