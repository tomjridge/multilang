all:
	sbt compile pack
	echo "Either add target/pack/bin/multilang to your PATH, or (cd target/pack && make) to install"
