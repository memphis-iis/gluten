@echo off

pip install --upgrade virtualenv
pip install --upgrade pip

virtualenv -p python3.exe env
cmd /C "env\Scripts\activate && pip install -r requirements.txt"
