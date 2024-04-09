if [[ "$1" = "NO-CACHE" ]]
then
   docker build --no-cache -f Dockerfile --tag ${PWD##*/}:latest .
else
   docker build -f Dockerfile --tag ${PWD##*/}:latest .
fi
