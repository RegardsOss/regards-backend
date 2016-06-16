from subprocess import call
import sys

if __name__=="__main__":
	try:
		cmd_tpl = 'ogr2ogr -f PostgreSQL PG:"dbname=geomars user=postgres password=password" {0}'
		cmd = cmd_tpl.format("costard_craters_min_3.json")
		print cmd
		retcode = call(cmd, shell=True)
		if retcode < 0:
			print >>sys.stderr, "Child was terminated by signal", -retcode
		else:
			print >>sys.stderr, "Child returned", retcode
	except OSError as e:
		print >>sys.stderr, "Execution failed:", e
		