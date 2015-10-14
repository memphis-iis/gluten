export GLUTEN_VENV="$HOME/.glutenenv"

if [ ! -d "$GLUTEN_VENV" ]; then
    mkdir -p "$GLUTEN_VENV"
fi
