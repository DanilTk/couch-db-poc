# Kill process by port number
lsof -i :8080 | awk 'NR>1{print $2}' | xargs -I {} kill -9 {}
