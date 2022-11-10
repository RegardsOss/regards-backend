#!/bin/bash -e

# Modifie le chemin vers les git hooks du microservice
# Param 1 : microservice name
function updateHookPath
{   
  microservice=$1
  cd $microservice
  git config core.hooksPath $REGARDS_HOME/rs-bom/regards-git-hooks
  cd ..
  echo "$microservice : hooksPath mis à jour."
}

if [[ -z "${REGARDS_HOME}" ]]
then
  echo "Erreur : La variable d'environnement REGARDS_HOME n'existe pas. REGARDS_HOME contient normalement le chemin vers le dossier regards-oss-backend."
  exit 1
fi

# REGARDS_HOME est sensé pointer vers le repo regards-oss-backend
cd $REGARDS_HOME
# On se place à la racine de tous les repos REGARDS
cd ..

# Cherche dans le répertoire courant les répertoires qui commencent par "regards-"
all_repos=`find . -maxdepth 1 -type d -name "regards-*"`

for repo in $all_repos
do
  updateHookPath $repo
done
