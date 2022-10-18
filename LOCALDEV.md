# Local Development
## Downstream dependencies
We use docker compose to standup all downstream dependencies required for our [Advanced Deployment Configurations](/ADVANCED.md)
with the following command : 
```docker compose -f localdev/docker-compose-dependecies.yml up```

## Running Diffy with localdev dependecies
We also use the following command line argument to point to corresponding configuration require to run Diffy:
```-Dspring.config.location=localdev.yml```
