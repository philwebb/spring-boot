COMMAND="gradle cleanEclipse eclipse -x :eclipse"
echo "About to run $COMMAND"
read
$COMMAND || exit

COMMAND="gradle :eclipse"
echo "About to run $COMMAND"
read
$COMMAND || exit
