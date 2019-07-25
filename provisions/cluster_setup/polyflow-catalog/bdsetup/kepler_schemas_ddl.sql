CREATE TYPE public."enum_prov_Entities_entity_type" AS ENUM (
    'provone_Data',
    'provone_Document',
    'provone_Visualization'
);

CREATE TYPE public."enum_provone_Ports_port_type" AS ENUM (
    'in',
    'out'
);

CREATE TABLE public.actor (
    id integer NOT NULL,
    class character varying(255) NOT NULL
);

CREATE TABLE public.actor_fire (
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    wf_exec_id integer NOT NULL,
    actor_id integer NOT NULL,
    id integer NOT NULL
);

CREATE TABLE public.actor_state (
    fire_id integer NOT NULL,
    state bytea NOT NULL
);

CREATE TABLE public.associated_data (
    val character varying(255) NOT NULL,
    data_id character varying(32) NOT NULL,
    name character varying(255) NOT NULL,
    wf_exec_id integer NOT NULL,
    id integer NOT NULL
);

CREATE TABLE public.data (
    contents bytea NOT NULL,
    truncated boolean NOT NULL,
    md5 character varying(32) NOT NULL
);

CREATE TABLE public.director (
    id integer NOT NULL,
    class character varying(255) NOT NULL
);

CREATE TABLE public.entity (
    deleted boolean DEFAULT false NOT NULL,
    wf_id integer NOT NULL,
    display character varying(255),
    name character varying(255),
    id integer NOT NULL,
    wf_change_id integer NOT NULL,
    type character varying(255) NOT NULL,
    prev_id integer NOT NULL
);

CREATE TABLE public.error (
    exec_id integer NOT NULL,
    id integer NOT NULL,
    entity_id integer,
    message character varying(255)
);

CREATE TABLE public.parameter (
    id integer NOT NULL,
    type character varying(255) NOT NULL,
    value character varying(255)
);

CREATE TABLE public.parameter_exec (
    wf_exec_id integer NOT NULL,
    parameter_id integer NOT NULL
);

CREATE TABLE public.port (
    multiport boolean NOT NULL,
    id integer NOT NULL,
    direction integer NOT NULL
);

CREATE TABLE public.port_event (
    write_event_id integer NOT NULL,
    data character varying(4096),
    data_id character varying(32),
    file_id character varying(32),
    channel integer NOT NULL,
    port_id integer NOT NULL,
    fire_id integer NOT NULL,
    id integer NOT NULL,
    "time" timestamp without time zone NOT NULL,
    type character varying(255)
);

CREATE TABLE public.tag (
    urn character varying(255) NOT NULL,
    wf_exec_id integer NOT NULL,
    id integer NOT NULL,
    type character varying(255) NOT NULL,
    searchstring character varying(255) NOT NULL
);


CREATE TABLE public.version_table (
    minor integer NOT NULL,
    version integer NOT NULL
);


CREATE TABLE public.workflow (
    lsid character varying(255) NOT NULL,
    name character varying(255),
    id integer NOT NULL
);

CREATE TABLE public.workflow_change (
    wf_id integer NOT NULL,
    id integer NOT NULL,
    "time" timestamp without time zone NOT NULL,
    "USER" character varying(255) NOT NULL,
    host_id character varying(255) NOT NULL
);

CREATE TABLE public.workflow_exec (
    annotation character varying(255),
    wf_contents_id character varying(32) NOT NULL,
    end_time timestamp without time zone NOT NULL,
    type character varying(255) DEFAULT 'unknown'::character varying NOT NULL,
    host_id character varying(255) NOT NULL,
    lsid character varying(255) NOT NULL,
    start_time timestamp without time zone NOT NULL,
    module_dependencies character varying(32768) NOT NULL,
    wf_id integer NOT NULL,
    derived_from character varying(255),
    wf_full_lsid character varying(255) NOT NULL,
    id integer NOT NULL,
    "USER" character varying(255) NOT NULL
);
