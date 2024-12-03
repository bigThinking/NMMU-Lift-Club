--
-- PostgreSQL database dump
--

-- Dumped from database version 9.3.3
-- Dumped by pg_dump version 9.3.3
-- Started on 2015-11-25 14:57:35

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 176 (class 3079 OID 11750)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 1960 (class 0 OID 0)
-- Dependencies: 176
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 170 (class 1259 OID 16406)
-- Name: clients; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE clients (
    client_id uuid,
    client_name text,
    client_type "char",
    client_location int2vector,
    client_logo uuid,
    "client_contact_No" character(10),
    client_email text,
    client_handler uuid
);


ALTER TABLE public.clients OWNER TO postgres;

--
-- TOC entry 1961 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_id IS 'primary key';


--
-- TOC entry 1962 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_name IS 'name of client';


--
-- TOC entry 1963 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_type IS 'place(p) or business(b)';


--
-- TOC entry 1964 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_location; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_location IS 'latitude & longitude of client';


--
-- TOC entry 1965 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_logo; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_logo IS 'resource uuid of logo in resources table';


--
-- TOC entry 1966 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients."client_contact_No"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients."client_contact_No" IS 'phone number of client';


--
-- TOC entry 1967 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_email; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_email IS 'email address of client';


--
-- TOC entry 1968 (class 0 OID 0)
-- Dependencies: 170
-- Name: COLUMN clients.client_handler; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN clients.client_handler IS 'UUID of handler in handler table
handler is person who handles the accounts for a client i.e pays bills
can handle 1 or more clients';


--
-- TOC entry 171 (class 1259 OID 16412)
-- Name: favourite; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE favourite (
    user_id uuid,
    client_id uuid
);


ALTER TABLE public.favourite OWNER TO postgres;

--
-- TOC entry 1969 (class 0 OID 0)
-- Dependencies: 171
-- Name: COLUMN favourite.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN favourite.user_id IS 'uuid of user who created favourite from user table
composite pk with client_id';


--
-- TOC entry 1970 (class 0 OID 0)
-- Dependencies: 171
-- Name: COLUMN favourite.client_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN favourite.client_id IS 'uuid of client who was favourited
composite pk with user_id';


--
-- TOC entry 172 (class 1259 OID 16415)
-- Name: handlers; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE handlers (
    handler_id uuid,
    handler_name text,
    payment_details text
);


ALTER TABLE public.handlers OWNER TO postgres;

--
-- TOC entry 1971 (class 0 OID 0)
-- Dependencies: 172
-- Name: COLUMN handlers.handler_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN handlers.handler_id IS 'primary key';


--
-- TOC entry 1972 (class 0 OID 0)
-- Dependencies: 172
-- Name: COLUMN handlers.payment_details; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN handlers.payment_details IS 'this should be one to many fields specifying payment details for this handler(don''t know what to put in yet)';


--
-- TOC entry 173 (class 1259 OID 16421)
-- Name: resource_types; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE resource_types (
    res_type_id uuid,
    res_type_name text,
    res_type_description text,
    res_type_cacheable boolean
);


ALTER TABLE public.resource_types OWNER TO postgres;

--
-- TOC entry 1973 (class 0 OID 0)
-- Dependencies: 173
-- Name: COLUMN resource_types.res_type_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resource_types.res_type_id IS 'UUID primary key';


--
-- TOC entry 1974 (class 0 OID 0)
-- Dependencies: 173
-- Name: COLUMN resource_types.res_type_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resource_types.res_type_name IS 'name of resource type';


--
-- TOC entry 1975 (class 0 OID 0)
-- Dependencies: 173
-- Name: COLUMN resource_types.res_type_description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resource_types.res_type_description IS 'description of resource type';


--
-- TOC entry 1976 (class 0 OID 0)
-- Dependencies: 173
-- Name: COLUMN resource_types.res_type_cacheable; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resource_types.res_type_cacheable IS 'can this resource type be cached on user device?';


--
-- TOC entry 174 (class 1259 OID 16427)
-- Name: resources; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE resources (
    res_id uuid,
    res_type uuid,
    res_created timestamp with time zone,
    res_last_modified timestamp with time zone,
    res_owner uuid,
    res_url text
);


ALTER TABLE public.resources OWNER TO postgres;

--
-- TOC entry 1977 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_id IS 'UUID primary key';


--
-- TOC entry 1978 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_type IS 'UUID of resource type';


--
-- TOC entry 1979 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_created; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_created IS 'Date/Time resouce was created';


--
-- TOC entry 1980 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_last_modified; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_last_modified IS 'Date/Time of last modified date';


--
-- TOC entry 1981 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_owner; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_owner IS 'UUID of client that owns resource';


--
-- TOC entry 1982 (class 0 OID 0)
-- Dependencies: 174
-- Name: COLUMN resources.res_url; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN resources.res_url IS 'location of file in filesystem or CDN';


--
-- TOC entry 175 (class 1259 OID 16433)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE users (
    user_id uuid,
    user_email text,
    user_cell character(10),
    user_pwd text
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 1983 (class 0 OID 0)
-- Dependencies: 175
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN users.user_id IS 'primary key';


--
-- TOC entry 1984 (class 0 OID 0)
-- Dependencies: 175
-- Name: COLUMN users.user_email; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN users.user_email IS 'email o user';


--
-- TOC entry 1985 (class 0 OID 0)
-- Dependencies: 175
-- Name: COLUMN users.user_cell; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN users.user_cell IS 'cellphone number of user';


--
-- TOC entry 1986 (class 0 OID 0)
-- Dependencies: 175
-- Name: COLUMN users.user_pwd; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN users.user_pwd IS 'password of user';


--
-- TOC entry 1959 (class 0 OID 0)
-- Dependencies: 6
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2015-11-25 14:57:36

--
-- PostgreSQL database dump complete
--

