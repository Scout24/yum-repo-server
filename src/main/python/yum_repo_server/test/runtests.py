# This file mainly exists to allow python setup.py test to work.
import subprocess

def runtests():
    returncode = subprocess.Popen(['python', 'manage.py', 'test', 'api', 'daemon']).wait()
    
    exit(returncode)

if __name__ == '__main__':
    runtests()