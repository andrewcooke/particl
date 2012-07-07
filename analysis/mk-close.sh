#!/bin/bash

dump() {

   lpath=$1
   n=$2
   tag=$3
   i=0

   pushd /home/andrew/project/particl/git

   while read line
   do
       echo $line
       s=`echo $line | sed -e 's/ /-/g' | sed -e 's/\..*//'`
       a=${line%% *}
       line=${line#* }
       b=${line%% *}
       out=`printf "/tmp/nbr-%02d--%s-%d-%s-%%d.png" $n $tag $i $s`
       echo "lein run -v -s hash --raw 0 --grey -a SHA-1 -n $n -o $out -i word $a $b"
       lein run -v -s hash --raw 0 --grey -a SHA-1 -n $n -o $out -i word $a $b
       (( i=$i + 1 ))
   done < $lpath

   popd
}

cd /home/andrew/project/particl/data
for n in 5 6 7 8 9 10 11
do
    best=`ls dump-${n}*.best`
    echo $best
    head -3 $best > /tmp/best
    dump /tmp/best $n "match"
    sort -n -k 4,5 $best | head -3 > /tmp/best
    dump /tmp/best $n "delta2"
done
