#!/bin/bash

if [[ $# != 2 ]]
then
    echo "mk-sample n tag"
    exit
fi

n=$1
tag=$2

pushd /home/andrew/project/particl/git
out=`printf "/tmp/sample-%02d-%s-%%d.png" $n $tag`

lein run -v -s hash --raw 0 --grey -a SHA-1 -n $n -o $out -i word a b c d e f g h i j k l m n o p q r s t u v w x y z

