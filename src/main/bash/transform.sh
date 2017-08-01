#!/bin/bash
DEVICE_ID=$1
cd /Users/akrause/gcrf-REAR/webapp/gcrfREAR/target/classes
CSVDIR=/Users/akrause/gcrf-REAR/data/snapshot-01Jul2017/new_csv
OUTDIR=$CSVDIR/$DEVICE_ID
mkdir -p $OUTDIR
FILES=/Users/akrause/gcrf-REAR/data/snapshot-01Jul2017/data/$DEVICE_ID/*
for f in $FILES
do
  echo "Processing $f file..."
  n=$(basename $f)
  outfile=$OUTDIR/$n.csv
  fn=$(java uk/ac/ed/epcc/rear/CSVWriter $f 2>&1 1 > $outfile)
  echo "Moved $n.csv to $fn.csv"
  mv $outfile $OUTDIR/$fn.csv
done
mv $OUTDIR $CSVDIR/$2
echo "Completed $CSVDIR/$2"