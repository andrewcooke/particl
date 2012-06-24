#!/bin/bash

if [[ $# -ne 2 ]]
then
    echo "mk-close.sh file.best n"
    exit 1
fi

path=$1
n=$2
i=0

cd /home/andrew/projects/personal/particl
old=`printf "/tmp/nbr-%02d-*.png" $n`
rm -f $old

while read line
do
     echo $line
     a=${line%% *}
     line=${line#* }
     b=${line%% *}
     line=${line#* }
     j=${line%% *}
     out=`printf "/tmp/nbr-%02d-%03d-%02d-%%d.png" $n $i $j`
     echo "lein run -v -s hash --raw 0 --grey -a SHA-1 -n $n -o $out -i word $a $b"
     lein run -v -s hash --raw 0 --grey -a SHA-1 -n $n -o $out -i word $a $b
     (( i=$i + 1 ))
done < $path
