-- catalog.islands: iid	scope_name	access_method
insert into catalog.islands values (0, 'RELATIONAL', 'PSQL');
insert into catalog.islands values (1, 'ARRAY', 'AFL');
insert into catalog.islands values (2, 'TEXT', 'JSON');
insert into catalog.islands values (3, 'API', 'JSON');
select setval('catalog.catalog.islands_iid_seq'::regclass, 2);

-- catalog.engines: engine_id, name, host, port, connection_properties
insert into catalog.engines values(0,'postgres0','bigdawg-postgres-catalog',5400,'PostgreSQL 9.4.5');
insert into catalog.engines values(1,'postgres1','bigdawg-postgres-kepler',5401,'PostgreSQL 9.4.5');
insert into catalog.engines values(2,'postgres2','bigdawg-postgres-swift',5402,'PostgreSQL 9.4.5');
-- insert into catalog.engines values(3,'scidb_local','bigdawg-scidb-data',1239,'SciDB 14.12');
-- insert into catalog.engines values (4, 'saw ZooKeeper', 'zookeeper.docker.local', 2181, 'Accumulo 1.6');
select setval('catalog.engines_eid_seq'::regclass, 4);

-- catalog.databases: dbid, engine_id, name, userid, password
insert into catalog.databases values(0,0,'bigdawg_catalog','postgres','test');
insert into catalog.databases values(1,0,'bigdawg_schemas','postgres','test');
insert into catalog.databases values(2,1,'kepler','postgres','test');
insert into catalog.databases values(3,2,'swift','postgres','test');
insert into catalog.databases values(4,0,'tpch','postgres','test');
insert into catalog.databases values(5,1,'tpch','postgres','test');
-- insert into catalog.databases values(6,3,'scidb_local','scidb','scidb123');
-- insert into catalog.databases values (7, 4, 'accumulo', 'bigdawg', 'bigdawg');
select setval('catalog.databases_dbid_seq'::regclass, 7);

-- catalog.shims: shim_id	island_id	engine_id	access_method
insert into catalog.shims values (0, 0, 0, 'N/A');
insert into catalog.shims values (1, 0, 1, 'N/A');
insert into catalog.shims values (2, 0, 2, 'N/A');
insert into catalog.shims values (3, 1, 3, 'N/A');
insert into catalog.shims values (4, 2, 4, 'N/A');
select setval('catalog.catalog.shims_shim_id_seq'::regclass, 4);

-- catalog.objects
-- Kepler tables
insert into catalog.objects values (1, 'actor', 'id,class', 2, 2);
insert into catalog.objects values (2, 'actor_fire', 'id,actor_id,wf_exec_time,end_time,start_time', 2, 2);
insert into catalog.objects values (3, 'actor_state', 'fire_id,state', 2, 2);
insert into catalog.objects values (4, 'associated_data', 'val,data_id,name,wf_exec_id,id', 2, 2);
insert into catalog.objects values (5, 'data', 'contents,truncated,md5', 2, 2);
insert into catalog.objects values (6, 'director', 'id,class', 2, 2);
insert into catalog.objects values (7, 'entity', 'deleted,wf_id,display,name,id,wf_change_id,type,prev_id', 2, 2);
insert into catalog.objects values (8, 'error', 'exec_id,id,entity_id,message', 2, 2);
insert into catalog.objects values (9, 'parameter', 'id,type,value', 2, 2);
insert into catalog.objects values (10, 'parameter_exec', 'wf_exec_id,parameter_id', 2, 2);
insert into catalog.objects values (11, 'port', 'multiport,id,direction', 2, 2);
insert into catalog.objects values (12, 'port_event', 'write_event_id,data,data_id,file_id,channel,port_id,fire_id,id,time,type', 2, 2);
insert into catalog.objects values (13, 'tag', 'urn,wf_exec_id,id,type,searchstring', 2, 2);
insert into catalog.objects values (14, 'version_table', 'minor,version', 2, 2);
insert into catalog.objects values (15, 'workflow', 'lsid,name,id', 2, 2);
insert into catalog.objects values (16, 'workflow_change', 'wf_id,id,time,USER,host_id', 2, 2);
insert into catalog.objects values (17, 'workflow_exec', 'annotation,wf_contents_id,end_time,type,host_id,lsid,start_time,module_dependencies,wf_id,derived_from,wf_full_lsid,id,USER', 2, 2);
-- Swift tables
insert into catalog.objects values (18, 'script_run', 'script_run_id,script_filename,log_filename,hostname,script_run_dir,swift_version,final_state,start_time,duration', 3, 3);
insert into catalog.objects values (19, 'script_run_argument', 'script_run_id,arg,value', 3, 3);
insert into catalog.objects values (20, 'app_exec', 'app_exec_id,script_run_id,app_name,execution_site,start_time,duration,staging_in_duration,staging_out_duration,work_directory', 3, 3);
insert into catalog.objects values (21, 'app_exec_argument', 'app_exec_id,arg_position,app_exec_arg', 3, 3);
insert into catalog.objects values (22, 'resource_usage', 'app_exec_id,real_secs,kernel_secs,user_secs,percent_cpu,max_rss,avg_rss,avg_tot_vm,avg_priv_data,avg_priv_stack,avg_shared_text,page_size,major_pgfaults,minor_pgfaults,swaps,invol_context_switches,vol_waits,fs_reads,fs_writes,sock_recv,sock_send,signals,exit_status', 3, 3);
insert into catalog.objects values (23, 'file', 'file_id,host,name,size,modify', 3, 3);
insert into catalog.objects values (24, 'staged_in', 'app_exec_id,file_id', 3, 3);
insert into catalog.objects values (25, 'staged_out', 'app_exec_id,file_id', 3, 3);

select setval('catalog.objects_oid_seq'::regclass, 48);
