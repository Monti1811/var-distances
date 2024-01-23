<h2>Results</h2>

The overall results can be inspected in [results_total.json](https://github.com/Monti1811/var-distances/blob/master/results_total.json).
These show the results of the distance and double declaration analysis over 31 of the biggest repositories in Java hosted on GitHub.
A detailed listing of the results can be found in [results.json](https://github.com/Monti1811/var-distances/blob/master/results.json).

Examples of those distances can be found in [examples.json](https://github.com/Monti1811/var-distances/blob/master/examples.json).
The lines, file and variable that is examined are described.

<h2>Information</h2>

The 31 repositories were chosen with the help of the paper [Dabic, O., Aghajani, E., & Bavota, G. (2021). Sampling Projects in GitHub for MSR Studies. 2021 IEEE/ACM 18th International Conference on Mining Software Repositories (MSR), 560-564](https://arxiv.org/pdf/2103.04682.pdf).

[Their search database](https://seart-ghs.si.usi.ch/) was used to determine the repositories.

As search parameters, I chose to search for repositories with at least 20 000 stars, at least 1000 commits and at least 100 000 lines of code.
The results of this search can be found in results_best_repos.json.

<h2>Functionality</h2>

This code uses [JavaParser](https://javaparser.org/) to analyze the code of each file of a repository. 

The resulting AST is parsed to find each declaration and usage of variables. Those are then mapped to their nearest neighbor, which produces the resulting distances.

<h2>Reproduction</h2>

The results can be reproduced by running [analyze_distances.py](https://github.com/Monti1811/var-distances/blob/master/analyze_distances.py) to get the distance and double declarations results.
By running [get_examples.py](https://github.com/Monti1811/var-distances/blob/master/get_examples.py), examples are found in the code.

One needs to download the repositories to their device and set the BASE_PATH_TO_REPOS variable to this path.
If changes to the repos are wanted, results_best_repos.json needs to be changed accordingly.

Tested with Python 3.8.
