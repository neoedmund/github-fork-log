## Github-Commits

A tool to list recent commits history on a specific github repo including all its forked repo.

## Method
Something like this:

* fetch all forks of original repo (a maxsize is set for large fork number like project linux)
* fetch recent orginal commits of each fork
* sort them by datetime and print

## run:
java -cp github-commits.jar neoe.github.LogForks `user/repo` `max-fork-scan`;


You will be asked to login github for up to 5,000 requests per hour instead of 60 requests per hour.
see https://developer.github.com/v3/#rate-limiting


## Reference

> Hi Neoe,
>  
> This API allows you to fetch the list of forks of a repository:
>  
> https://developer.github.com/v3/repos/forks/#list-forks
>  
> Each item in the list has a full_name property which gives you the name of the repository.
>  
> Example:
>  
> curl -v https://api.github.com/repos/atom/atom/forks
>  
> Also, make sure you read the docs on pagination:
>  
> https://developer.github.com/v3/#pagination
>  
> Hope this helps.
>  
> Cheers,
> Ivan


