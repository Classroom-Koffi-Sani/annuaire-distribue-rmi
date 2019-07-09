#!/usr/bin/env python

import time
import sys
import os

def is_correct_nb_slaves(number):
	checked_number = number + 2
	if checked_number > 2 and checked_number & (checked_number-1) == 0:
		return True
	else:
		return False
		
def are_valid_parameters(params):
	if len(params) == 4:
		return True
	else:
		return False

def kill_tinydfs_processes():
	java_processes = os.popen('ps aux | grep java').read()
	line_java_processes = java_processes.split("\n")
	for line in line_java_processes:
		if 'MasterMain' in line or 'SlaveMain' in line:
			words = line.split(' ')
			done = False
			for word in words:
				if done:
					break
				process_id = 0
				try:
					process_id = int(word)
				except:
					done = False
				if process_id != 0:
					command = 'kill -9 ' + str(process_id)
					print '[script] Running command: ' + command
					os.system(command)
					done = True
				

if __name__ == '__main__':
	master_host = 'localhost'
	nb_args	= len(sys.argv)
	nb_slaves = sys.argv[3]
	if not are_valid_parameters(sys.argv) or not is_correct_nb_slaves(int(nb_slaves)):
		print 'Usage:\t python ' + sys.argv[0] + ' storage_service_name dfs_root_folder nb_slaves'
		print 'Note:\t the number of slaves + 2 must be a power of two.'
		sys.exit(0)
		
	kill_tinydfs_processes()
	storage_service_name = sys.argv[1]
	dfs_root_folder = sys.argv[2]
	print ''
	print '[script] Starting storage service: ' + storage_service_name
	print '[script] DFS root folder: ' + dfs_root_folder
	print '[script] Number of slaves: ' + str(nb_slaves)
	print ''
	
	command = 'java -cp target/classes fr.unice.miage.sd.tinydfs.main.MasterMain'
	command_with_parameters = command + ' ' + storage_service_name + ' ' + dfs_root_folder + ' ' + nb_slaves + ' &'
	os.system(command_with_parameters)
	# The following sleep is needed to wait for the rmiregistry to be set up		
	time.sleep(1)
	for i in range(int(nb_slaves)):
		command = 'java -cp target/classes fr.unice.miage.sd.tinydfs.main.SlaveMain'
		command_with_parameters = command + ' ' + master_host + ' ' + dfs_root_folder + ' ' + str(i) + ' &'
		os.system(command_with_parameters)
		
	time.sleep(5)
	print '[script] Running clients...'
	command = 'java -cp target/test-classes fr.unice.miage.sd.tinydfs.tests.config.PropertiesWriter'
	command_with_parameters = command + ' ' + storage_service_name + ' ' + master_host
	os.system(command_with_parameters)
	command = 'mvn test'
	os.system(command)
		


