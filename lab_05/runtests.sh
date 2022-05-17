#!/bin/bash
for i in {1..12} # broj ispitnih primjera
do
	# generiraj ime direktorija s vodećom nulom
	dir=$(printf "%0*d\n" 2 $i)
	echo "Test $dir"
	# pokreni program i provjeri izlaz
	res=`java ./src/GNAlgorithm.java < examples/test$dir/R.in | diff examples/test$dir/R.out -`
	if [ "$res" != "" ]
	then
		# izlazi ne odgovaraju
		echo "FAIL"
		echo $res
	else
		# OK!
		echo "OK"
	fi
done
