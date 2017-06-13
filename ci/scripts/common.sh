setup_symlinks() {
	mkdir -p m2 && ln -s "$(pwd)/m2" ~/.m2
	mkdir -p gradle && ln -s "$(pwd)/gradle" ~/.gradle
}

cleanup_maven_repo() {
	rm -fr ~/.m2/repository/org/springframework/boot 2> /dev/null || :
}

run_maven() {
	echo "./mvnw $@" > build.log
	((tail -n 0 -q -f build.log & echo $! >&3) 3>pid | grep -v --color=never "\] \[INFO\]\|Building\ jar" | grep --color=never "\[INFO.*---\|Building\ \|SUCCESS\|Total\ time\|Finished\ at\|Final\ Memory" &)
	echo "./mvnw $@"
	./mvnw "$@" >> build.log 2>&1 || (kill $(<pid) && sleep 1 && tail -n 3000 build.log && exit 1)
	kill $(<pid)
	sleep 1
}