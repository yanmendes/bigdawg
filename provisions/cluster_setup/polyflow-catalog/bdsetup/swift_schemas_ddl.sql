CREATE TABLE script_run (
	script_run_id	   varchar(255) primary key,
        script_filename    text,
	log_filename       text,
	hostname	   text,
	script_run_dir	   text,
        swift_version      text,
        final_state        text,
        start_time         text,
        duration           real
);
CREATE TABLE script_run_argument (
			script_run_id		varchar(255) references script_run (script_run_id),
			arg				text,
			value			text
);
CREATE TABLE app_exec (
	app_exec_id			varchar(255) primary key,
  script_run_id   		varchar(255) references script_run(script_run_id),
	app_name			text,
	execution_site			text,
	start_time			text,
	duration			real,
	staging_in_duration		real,
	staging_out_duration		real,
	work_directory			text
);
CREATE TABLE app_exec_argument (
	app_exec_id			varchar(255) references app_exec (app_exec_id),
	arg_position			integer,
	app_exec_arg			text
);
CREATE TABLE resource_usage (
       app_exec_id	    		varchar(255) primary key references app_exec (app_exec_id),
       real_secs	       		real,
       kernel_secs             		real,
       user_secs	       		real,
       percent_cpu             		integer,
       max_rss	       	       		integer,
       avg_rss	       			integer,
       avg_tot_vm	       		integer,
       avg_priv_data     		integer,
       avg_priv_stack    		integer,
       avg_shared_text   		integer,
       page_size	       		integer,
       major_pgfaults    		integer,
       minor_pgfaults    		integer,
       swaps	       			integer,
       invol_context_switches		integer,
       vol_waits			integer,
       fs_reads				integer,
       fs_writes			integer,
       sock_recv			integer,
       sock_send			integer,
       signals				integer,
       exit_status			integer
);
CREATE TABLE file (
       file_id		varchar(255) primary key,
       host		text,
       name		text,
       size		integer,
       modify		integer
);
CREATE TABLE staged_in (
       app_exec_id			varchar(255) references app_exec (app_exec_id),
       file_id 				varchar(255) references file (file_id)
);
CREATE TABLE staged_out (
       app_exec_id			varchar(255) references app_exec (app_exec_id),
       file_id				varchar(255) references file (file_id)
);
