<h2>Why?</h2>

This project was created to find out how variables are declared and used in Java code.
The goal was to find out how the distance between the declaration and the first usage/first declaration without a usage of a variable is distributed.
As it may be that the distance between variable declaration and usage may influence the code comprehension of a reader, 
it was interesting to see how this distance is distributed.

<h2>Results</h2>

The overall results can be inspected in [results_total.json](https://github.com/Monti1811/var-distances/blob/master/results_total.json).
These show the results of the distance and double declaration analysis over 31 of the biggest repositories in Java hosted on GitHub.
A detailed listing of the results can be found in [results.json](https://github.com/Monti1811/var-distances/blob/master/results.json).

Examples of those distances can be found in [good_examples.json](https://github.com/Monti1811/var-distances/blob/master/good_examples.json).
The lines, file and variable that are examined are described in the file.

A quick overview over the results is here:

|            | Declaration/Usage | Declaration/Declaration |
|------------|-------------------|-------------------------|
| **Total**  | 1243304           | 67319                   |
| **Mean**   | 3.77              | 6.90                    |
| **Median** | 1                 | 3                       |

An example of code found in the repositories is:

```java
// Declaration/Usage of the variable holder
@Nullable TypeName baseClass = baseClasses.any() ? tname(packageName + "." + baseName(baseClassType)) : null; // "baseClass" first declared here
@Nullable TypeSpec.Builder baseClassBuilder = baseClassType == null ? null : this.baseClasses.find(b -> Reflect.<String>get(b, "name").equals(baseName(baseClassType)));
boolean addIndexToBase = baseClassBuilder != null && baseClassIndexers.add(baseClassBuilder);
//whether the main class is the base itself
boolean typeIsBase = baseClassType != null && type.has(Component.class) && type.annotation(Component.class).base();

if(type.isType() && (!type.name().endsWith("Def") && !type.name().endsWith("Comp"))){
    err("All entity def names must end with 'Def'/'Comp'", type.e);
}

String name = type.isType() ?
    type.name().replace("Def", "").replace("Comp", "") :
    createName(type);

//check for type name conflicts
if(!typeIsBase && baseClass != null && name.equals(baseName(baseClassType))){ // "baseClass" first used here
    name += "Entity";
}
```


<h2>Information</h2>

The 31 repositories were chosen with the help of the paper [Dabic, O., Aghajani, E., & Bavota, G. (2021). Sampling Projects in GitHub for MSR Studies. 2021 IEEE/ACM 18th International Conference on Mining Software Repositories (MSR), 560-564](https://arxiv.org/pdf/2103.04682.pdf).

[Their search database](https://seart-ghs.si.usi.ch/) was used to determine the repositories.

As search parameters, I chose to search for repositories with at least 20 000 stars, at least 1000 commits and at least 100 000 lines of code.
The results of this search can be found in [results_best_repos.json](https://github.com/Monti1811/var-distances/blob/master/results_best_repos.json).

<h2>Functionality</h2>

This code uses [JavaParser](https://javaparser.org/) to analyze the code of each file of a repository. 

The resulting AST is parsed to find each declaration and usage of variables. Those are then mapped to their nearest neighbor, which produces the resulting distances.

<h3>Limitations</h3>

This code produces only rough results, as it does not take into account the scope of the variable.
That means,
if a variable is defined in the branch of a if-statement,
it can also be mapped to a variable defined in the else-branch of the if-statement.
The same can be said for switch-statements and loops.
Additionally, the code counts variables as declared even if they are declared without a value.

<h2>Reproduction</h2>

The results can be reproduced by running [analyze_distances.py](https://github.com/Monti1811/var-distances/blob/master/analyze_distances.py) to get the distance and double declarations results.
By running [get_examples.py](https://github.com/Monti1811/var-distances/blob/master/get_examples.py), examples are found in the code.

One needs to download the repositories to their device and set the BASE_PATH_TO_REPOS variable to this path.
If changes to the repos are wanted, results_best_repos.json needs to be changed accordingly.

Tested with Python 3.8.
