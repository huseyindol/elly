--
-- PostgreSQL database dump
--

\restrict I3ibXuejYVZ92yoNDahtxTuC5hrxaGHZejuIneG25EuWvdJ07CByaBJNLGxH2xb

-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY elly.component_forms DROP CONSTRAINT IF EXISTS fkr7eas7bmrp0oy59xx18r2btgv;
ALTER TABLE IF EXISTS ONLY elly.widget_banners DROP CONSTRAINT IF EXISTS fkqp8ibilv4fv8pywrgn1h847op;
ALTER TABLE IF EXISTS ONLY elly.cms_contents DROP CONSTRAINT IF EXISTS fko0xv9diurwi1ltwbceg7t694c;
ALTER TABLE IF EXISTS ONLY elly.component_forms DROP CONSTRAINT IF EXISTS fknlk3jjp9ybylq5ra6x1ygjj8a;
ALTER TABLE IF EXISTS ONLY elly.widget_banners DROP CONSTRAINT IF EXISTS fklsci7uxy4fqqj23t52huyis42;
ALTER TABLE IF EXISTS ONLY elly.pages DROP CONSTRAINT IF EXISTS fkl9l1wjycqrl62d4r8jn3u4nu5;
ALTER TABLE IF EXISTS ONLY elly.ratings DROP CONSTRAINT IF EXISTS fkl38egjrojmh8hq3uoyo7is8h6;
ALTER TABLE IF EXISTS ONLY elly.page_components DROP CONSTRAINT IF EXISTS fkk0cqx20neichlg97egj3jgqg0;
ALTER TABLE IF EXISTS ONLY elly.comments DROP CONSTRAINT IF EXISTS fkh4c7lvsc298whoyd4w9ta25cr;
ALTER TABLE IF EXISTS ONLY elly.page_components DROP CONSTRAINT IF EXISTS fkdfob1d9oq7vfmb8o67n27vohg;
ALTER TABLE IF EXISTS ONLY elly.component_banners DROP CONSTRAINT IF EXISTS fkcnj5y54vcky4w0ww2gvt87y7g;
ALTER TABLE IF EXISTS ONLY elly.component_widgets DROP CONSTRAINT IF EXISTS fkcenu7fw3b2eo8pv0oo48hqunc;
ALTER TABLE IF EXISTS ONLY elly.component_widgets DROP CONSTRAINT IF EXISTS fkc2i9net1t9vlm4dmbcm5mlcsj;
ALTER TABLE IF EXISTS ONLY elly.posts DROP CONSTRAINT IF EXISTS fkb7fka510air19xqvbxtod9ht0;
ALTER TABLE IF EXISTS ONLY elly.widget_posts DROP CONSTRAINT IF EXISTS fka8ax06lro5qx37mv5yo08l1ck;
ALTER TABLE IF EXISTS ONLY elly.comments DROP CONSTRAINT IF EXISTS fk7h839m3lkvhbyv3bcdv7sm4fj;
ALTER TABLE IF EXISTS ONLY elly.component_banners DROP CONSTRAINT IF EXISTS fk5pb7bbbtpn4722qh2d75mlpnx;
ALTER TABLE IF EXISTS ONLY elly.widget_posts DROP CONSTRAINT IF EXISTS fk3ssrfk5po43ldy0rtd0jmx1a8;
ALTER TABLE IF EXISTS ONLY elly.form_submissions DROP CONSTRAINT IF EXISTS fk2kxl75gxcdqrjh4n9k5qo27q8;
ALTER TABLE IF EXISTS ONLY elly.refresh_tokens DROP CONSTRAINT IF EXISTS fk1lih5y2npsf8u5o3vhdb9y0os;
DROP INDEX IF EXISTS elly.idx_refresh_token_user_id;
DROP INDEX IF EXISTS elly.idx_rating_user_post;
DROP INDEX IF EXISTS elly.idx_rating_post_id;
DROP INDEX IF EXISTS elly.idx_post_status;
DROP INDEX IF EXISTS elly.idx_post_seo_info_id;
DROP INDEX IF EXISTS elly.idx_form_sub_submitted_at;
DROP INDEX IF EXISTS elly.idx_form_sub_form_id;
DROP INDEX IF EXISTS elly.idx_form_def_title;
DROP INDEX IF EXISTS elly.idx_form_def_active;
DROP INDEX IF EXISTS elly.idx_email_status_created;
DROP INDEX IF EXISTS elly.idx_email_status;
DROP INDEX IF EXISTS elly.idx_comment_status;
DROP INDEX IF EXISTS elly.idx_comment_post_status;
DROP INDEX IF EXISTS elly.idx_comment_post_id;
DROP INDEX IF EXISTS elly.idx_comment_parent_id;
DROP INDEX IF EXISTS elly.idx_cms_content_section_key;
DROP INDEX IF EXISTS elly.idx_cms_content_content_type;
DROP INDEX IF EXISTS elly.idx_cms_content_active;
DROP INDEX IF EXISTS elly.idx_cms_basic_info_section_key;
DROP INDEX IF EXISTS elly.idx_cms_basic_info_active;
DROP INDEX IF EXISTS elly.id_widget_type_status;
DROP INDEX IF EXISTS elly.id_widget_type_name;
DROP INDEX IF EXISTS elly.id_widget_type;
DROP INDEX IF EXISTS elly.id_widget_name;
DROP INDEX IF EXISTS elly.id_seoinfo_title;
DROP INDEX IF EXISTS elly.id_seoinfo_canonical_url;
DROP INDEX IF EXISTS elly.id_page_status;
DROP INDEX IF EXISTS elly.id_component_type_status;
DROP INDEX IF EXISTS elly.id_component_type_name;
DROP INDEX IF EXISTS elly.id_component_type;
DROP INDEX IF EXISTS elly.id_component_name;
DROP INDEX IF EXISTS elly.id_banner_title;
DROP INDEX IF EXISTS elly.id_banner_status;
ALTER TABLE IF EXISTS ONLY elly.widgets DROP CONSTRAINT IF EXISTS widgets_pkey;
ALTER TABLE IF EXISTS ONLY elly.widget_posts DROP CONSTRAINT IF EXISTS widget_posts_pkey;
ALTER TABLE IF EXISTS ONLY elly.widget_banners DROP CONSTRAINT IF EXISTS widget_banners_pkey;
ALTER TABLE IF EXISTS ONLY elly.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY elly.pages DROP CONSTRAINT IF EXISTS ukop7mbivq89lx29uoeta2o6hc1;
ALTER TABLE IF EXISTS ONLY elly.posts DROP CONSTRAINT IF EXISTS ukml7eiucwpy3gkvs30195gvmdl;
ALTER TABLE IF EXISTS ONLY elly.assets DROP CONSTRAINT IF EXISTS ukln4kj492rlnemwox5b7x38ld7;
ALTER TABLE IF EXISTS ONLY elly.refresh_tokens DROP CONSTRAINT IF EXISTS uk7tdcd6ab5wsgoudnvj7xf1b7l;
ALTER TABLE IF EXISTS ONLY elly.users DROP CONSTRAINT IF EXISTS uc_user_username;
ALTER TABLE IF EXISTS ONLY elly.users DROP CONSTRAINT IF EXISTS uc_user_email;
ALTER TABLE IF EXISTS ONLY elly.ratings DROP CONSTRAINT IF EXISTS uc_rating_user_post;
ALTER TABLE IF EXISTS ONLY elly.posts DROP CONSTRAINT IF EXISTS uc_post_slug;
ALTER TABLE IF EXISTS ONLY elly.pages DROP CONSTRAINT IF EXISTS uc_page_slug;
ALTER TABLE IF EXISTS ONLY elly.seo_infos DROP CONSTRAINT IF EXISTS seo_infos_pkey;
ALTER TABLE IF EXISTS ONLY elly.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;
ALTER TABLE IF EXISTS ONLY elly.ratings DROP CONSTRAINT IF EXISTS ratings_pkey;
ALTER TABLE IF EXISTS ONLY elly.posts DROP CONSTRAINT IF EXISTS posts_pkey;
ALTER TABLE IF EXISTS ONLY elly.pages DROP CONSTRAINT IF EXISTS pages_pkey;
ALTER TABLE IF EXISTS ONLY elly.page_components DROP CONSTRAINT IF EXISTS page_components_pkey;
ALTER TABLE IF EXISTS ONLY elly.users DROP CONSTRAINT IF EXISTS idx_user_username;
ALTER TABLE IF EXISTS ONLY elly.users DROP CONSTRAINT IF EXISTS idx_user_email;
ALTER TABLE IF EXISTS ONLY elly.refresh_tokens DROP CONSTRAINT IF EXISTS idx_refresh_token_token;
ALTER TABLE IF EXISTS ONLY elly.posts DROP CONSTRAINT IF EXISTS idx_post_slug;
ALTER TABLE IF EXISTS ONLY elly.pages DROP CONSTRAINT IF EXISTS id_page_page_slug;
ALTER TABLE IF EXISTS ONLY elly.form_submissions DROP CONSTRAINT IF EXISTS form_submissions_pkey;
ALTER TABLE IF EXISTS ONLY elly.form_definitions DROP CONSTRAINT IF EXISTS form_definitions_pkey;
ALTER TABLE IF EXISTS ONLY elly.email_logs DROP CONSTRAINT IF EXISTS email_logs_pkey;
ALTER TABLE IF EXISTS ONLY elly.components DROP CONSTRAINT IF EXISTS components_pkey;
ALTER TABLE IF EXISTS ONLY elly.component_widgets DROP CONSTRAINT IF EXISTS component_widgets_pkey;
ALTER TABLE IF EXISTS ONLY elly.component_forms DROP CONSTRAINT IF EXISTS component_forms_pkey;
ALTER TABLE IF EXISTS ONLY elly.component_banners DROP CONSTRAINT IF EXISTS component_banners_pkey;
ALTER TABLE IF EXISTS ONLY elly.comments DROP CONSTRAINT IF EXISTS comments_pkey;
ALTER TABLE IF EXISTS ONLY elly.cms_contents DROP CONSTRAINT IF EXISTS cms_contents_pkey;
ALTER TABLE IF EXISTS ONLY elly.cms_basic_infos DROP CONSTRAINT IF EXISTS cms_basic_infos_pkey;
ALTER TABLE IF EXISTS ONLY elly.banners DROP CONSTRAINT IF EXISTS banners_pkey;
ALTER TABLE IF EXISTS ONLY elly.assets DROP CONSTRAINT IF EXISTS assets_pkey;
DROP TABLE IF EXISTS elly.widgets;
DROP TABLE IF EXISTS elly.widget_posts;
DROP TABLE IF EXISTS elly.widget_banners;
DROP TABLE IF EXISTS elly.users;
DROP TABLE IF EXISTS elly.seo_infos;
DROP TABLE IF EXISTS elly.refresh_tokens;
DROP TABLE IF EXISTS elly.ratings;
DROP TABLE IF EXISTS elly.posts;
DROP TABLE IF EXISTS elly.pages;
DROP TABLE IF EXISTS elly.page_components;
DROP TABLE IF EXISTS elly.form_submissions;
DROP TABLE IF EXISTS elly.form_definitions;
DROP TABLE IF EXISTS elly.email_logs;
DROP TABLE IF EXISTS elly.components;
DROP TABLE IF EXISTS elly.component_widgets;
DROP TABLE IF EXISTS elly.component_forms;
DROP TABLE IF EXISTS elly.component_banners;
DROP TABLE IF EXISTS elly.comments;
DROP TABLE IF EXISTS elly.cms_contents;
DROP TABLE IF EXISTS elly.cms_basic_infos;
DROP TABLE IF EXISTS elly.banners;
DROP TABLE IF EXISTS elly.assets;
DROP SCHEMA IF EXISTS elly;
--
-- Name: elly; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA elly;


ALTER SCHEMA elly OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: assets; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.assets (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    extension character varying(255),
    name character varying(255),
    path character varying(255),
    sub_folder character varying(255),
    type character varying(255)
);


ALTER TABLE elly.assets OWNER TO postgres;

--
-- Name: assets_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.assets ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.assets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: banners; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.banners (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    alt_text character varying(255),
    desktop character varying(255),
    mobile character varying(255),
    tablet character varying(255),
    link character varying(255),
    order_index integer,
    status boolean,
    sub_folder character varying(255),
    target character varying(255),
    title character varying(255),
    type character varying(255)
);


ALTER TABLE elly.banners OWNER TO postgres;

--
-- Name: banners_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.banners ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.banners_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cms_basic_infos; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.cms_basic_infos (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    is_active boolean,
    section_key character varying(255),
    sort_order integer,
    title character varying(255),
    updated_at timestamp(6) without time zone
);


ALTER TABLE elly.cms_basic_infos OWNER TO postgres;

--
-- Name: cms_contents; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.cms_contents (
    id uuid NOT NULL,
    content_type character varying(255),
    created_at timestamp(6) without time zone,
    metadata jsonb,
    updated_at timestamp(6) without time zone,
    basic_info_id uuid,
    description character varying(255),
    is_active boolean,
    section_key character varying(255),
    sort_order integer,
    title character varying(255)
);


ALTER TABLE elly.cms_contents OWNER TO postgres;

--
-- Name: comments; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.comments (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content character varying(255),
    email character varying(255) NOT NULL,
    name character varying(255),
    status boolean,
    parent_comment_id bigint,
    post_id bigint
);


ALTER TABLE elly.comments OWNER TO postgres;

--
-- Name: comments_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.comments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: component_banners; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.component_banners (
    component_id bigint NOT NULL,
    banner_id bigint NOT NULL
);


ALTER TABLE elly.component_banners OWNER TO postgres;

--
-- Name: component_forms; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.component_forms (
    component_id bigint NOT NULL,
    form_definition_id bigint NOT NULL
);


ALTER TABLE elly.component_forms OWNER TO postgres;

--
-- Name: component_widgets; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.component_widgets (
    component_id bigint NOT NULL,
    widget_id bigint NOT NULL
);


ALTER TABLE elly.component_widgets OWNER TO postgres;

--
-- Name: components; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.components (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content character varying(255),
    description character varying(255),
    name character varying(255),
    order_index integer,
    status boolean,
    template character varying(255),
    type smallint,
    CONSTRAINT components_type_check CHECK (((type >= 0) AND (type <= 1)))
);


ALTER TABLE elly.components OWNER TO postgres;

--
-- Name: components_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.components ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.components_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: email_logs; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.email_logs (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    error_message text,
    payload_json text,
    recipient character varying(255) NOT NULL,
    retry_count integer NOT NULL,
    sent_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    subject character varying(255) NOT NULL,
    template_name character varying(255) NOT NULL,
    CONSTRAINT email_logs_status_check CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('SENT'::character varying)::text, ('FAILED'::character varying)::text])))
);


ALTER TABLE elly.email_logs OWNER TO postgres;

--
-- Name: email_logs_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.email_logs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.email_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: form_definitions; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.form_definitions (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    active boolean,
    schema jsonb,
    title character varying(255),
    version integer
);


ALTER TABLE elly.form_definitions OWNER TO postgres;

--
-- Name: form_definitions_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.form_definitions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.form_definitions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: form_submissions; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.form_submissions (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    payload jsonb,
    submitted_at timestamp(6) without time zone,
    form_definition_id bigint
);


ALTER TABLE elly.form_submissions OWNER TO postgres;

--
-- Name: form_submissions_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.form_submissions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.form_submissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: page_components; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.page_components (
    page_id bigint NOT NULL,
    component_id bigint NOT NULL
);


ALTER TABLE elly.page_components OWNER TO postgres;

--
-- Name: pages; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.pages (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    description character varying(255),
    slug character varying(255),
    status boolean,
    template character varying(255),
    title character varying(255),
    seo_info_id bigint
);


ALTER TABLE elly.pages OWNER TO postgres;

--
-- Name: pages_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.pages ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.pages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: posts; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.posts (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content text,
    order_index integer,
    slug character varying(255) NOT NULL,
    status boolean,
    template character varying(255),
    title character varying(255),
    seo_info_id bigint
);


ALTER TABLE elly.posts OWNER TO postgres;

--
-- Name: posts_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.posts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.posts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ratings; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.ratings (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    comment character varying(500),
    rating integer NOT NULL,
    user_identifier character varying(255) NOT NULL,
    post_id bigint NOT NULL
);


ALTER TABLE elly.ratings OWNER TO postgres;

--
-- Name: ratings_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.ratings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.ratings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.refresh_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    expiry_date timestamp(6) without time zone NOT NULL,
    is_revoked boolean NOT NULL,
    token character varying(500) NOT NULL,
    user_id bigint NOT NULL
);


ALTER TABLE elly.refresh_tokens OWNER TO postgres;

--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.refresh_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: seo_infos; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.seo_infos (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    canonical_url character varying(255),
    description character varying(255),
    keywords character varying(255),
    no_follow boolean,
    no_index boolean,
    title character varying(255)
);


ALTER TABLE elly.seo_infos OWNER TO postgres;

--
-- Name: seo_infos_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.seo_infos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.seo_infos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.users (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    email character varying(255) NOT NULL,
    first_name character varying(255),
    is_active boolean NOT NULL,
    last_name character varying(255),
    password character varying(255),
    provider character varying(255),
    provider_id character varying(255),
    token_version bigint NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE elly.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: widget_banners; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.widget_banners (
    widget_id bigint NOT NULL,
    banner_id bigint NOT NULL
);


ALTER TABLE elly.widget_banners OWNER TO postgres;

--
-- Name: widget_posts; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.widget_posts (
    widget_id bigint NOT NULL,
    post_id bigint NOT NULL
);


ALTER TABLE elly.widget_posts OWNER TO postgres;

--
-- Name: widgets; Type: TABLE; Schema: elly; Owner: postgres
--

CREATE TABLE elly.widgets (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    content character varying(255),
    description character varying(255),
    name character varying(255),
    order_index integer,
    status boolean,
    template character varying(255),
    type smallint,
    CONSTRAINT widgets_type_check CHECK (((type >= 0) AND (type <= 1)))
);


ALTER TABLE elly.widgets OWNER TO postgres;

--
-- Name: widgets_id_seq; Type: SEQUENCE; Schema: elly; Owner: postgres
--

ALTER TABLE elly.widgets ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME elly.widgets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Data for Name: assets; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.assets (id, created_at, updated_at, extension, name, path, sub_folder, type) FROM stdin;
15	2026-02-06 03:24:56.751	2026-02-06 03:24:56.751	.png	Big-Bass-Splash-1000.png	assets/Big-Bass-Splash-1000.png	\N	image/png
16	2026-02-06 03:24:56.824	2026-02-06 03:24:56.824	.png	Fortune-Of-Olympus.png	assets/Fortune-Of-Olympus.png	\N	image/png
17	2026-02-06 03:24:56.861	2026-02-06 03:24:56.861	.png	Gate-of-Olympus-Super-Scatter.png	assets/Gate-of-Olympus-Super-Scatter.png	\N	image/png
18	2026-02-06 03:24:56.893	2026-02-06 03:24:56.893	.png	Hot-Tuna.png	assets/Hot-Tuna.png	\N	image/png
19	2026-02-06 03:24:56.919	2026-02-06 03:24:56.919	.png	ngsbahis-bonanza.png	assets/ngsbahis-bonanza.png	\N	image/png
20	2026-02-06 03:24:56.946	2026-02-06 03:24:56.946	.png	Sugar-Rush-Super-Scatter.png	assets/Sugar-Rush-Super-Scatter.png	\N	image/png
21	2026-02-06 03:24:56.971	2026-02-06 03:24:56.971	.png	Sweet-Rush-Bonanza.png	assets/Sweet-Rush-Bonanza.png	\N	image/png
22	2026-02-08 04:30:05.73	2026-02-08 04:30:05.73	.png	Big-Bass-Splash-1000.png	assets/oyunlar/Big-Bass-Splash-1000.png	oyunlar	image/png
23	2026-02-08 04:30:05.795	2026-02-08 04:30:05.795	.png	Fortune-Of-Olympus.png	assets/oyunlar/Fortune-Of-Olympus.png	oyunlar	image/png
24	2026-02-08 04:30:05.817	2026-02-08 04:30:05.817	.png	Gate-of-Olympus-Super-Scatter.png	assets/oyunlar/Gate-of-Olympus-Super-Scatter.png	oyunlar	image/png
25	2026-02-08 04:30:05.843	2026-02-08 04:30:05.843	.png	Hot-Tuna.png	assets/oyunlar/Hot-Tuna.png	oyunlar	image/png
26	2026-02-08 04:30:05.864	2026-02-08 04:30:05.864	.png	ngsbahis-bonanza.png	assets/oyunlar/ngsbahis-bonanza.png	oyunlar	image/png
27	2026-02-08 04:30:05.892	2026-02-08 04:30:05.892	.png	Sugar-Rush-Super-Scatter.png	assets/oyunlar/Sugar-Rush-Super-Scatter.png	oyunlar	image/png
28	2026-02-08 04:30:05.92	2026-02-08 04:30:05.92	.png	Sweet-Rush-Bonanza.png	assets/oyunlar/Sweet-Rush-Bonanza.png	oyunlar	image/png
\.


--
-- Data for Name: banners; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.banners (id, created_at, updated_at, alt_text, desktop, mobile, tablet, link, order_index, status, sub_folder, target, title, type) FROM stdin;
2	2026-01-22 23:38:51.075	2026-01-22 23:38:51.075	test2	https://fotolifeakademi.com/uploads/2020/04/manzara-fotografi-cekmek.jpg	https://www.posteratolyesi.com/img/products/manzara-duvar-kagidi-250-b_13.12.2020_1a31f33.jpg	https://blog.obilet.com/wp-content/uploads/2023/07/0anagorsel-1-scaled.jpeg	https://example.com	0	t	oyunlar	_blank	test2	test2
1	2026-01-22 23:30:56.073	2026-01-23 00:05:39.189	test	assets/images/banners/oyunlar/desktop/7c49e84c-c40e-4591-b670-57dc5a0bc745.png	assets/images/banners/oyunlar/mobile/0ac45057-ec93-462c-99a9-48b4ad7607b8.png	assets/images/banners/oyunlar/tablet/0ba78d26-8cae-4105-9940-3f969a2eb190.png	https://example.com	1	t	oyunlar	_blank	test	test
\.


--
-- Data for Name: cms_basic_infos; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.cms_basic_infos (id, created_at, description, is_active, section_key, sort_order, title, updated_at) FROM stdin;
97608fe6-a3eb-4111-8972-e48809dd272f	2026-02-23 01:14:00.815	10+ yıllık deneyimimde uzmanlaştığım teknolojiler ve seviyeler	t	portfolio_skills	1	Teknolojiler & Yetenekler	2026-02-23 01:14:00.815
fc90b992-dc1a-402e-8465-ead5fcc64b68	2026-02-23 02:14:57.235	Çeşitli sektörlerde edindiğim deneyimler ve başarılar	t	portfolio_experience	2	Profesyonel Deneyim	2026-02-23 02:14:57.235
\.


--
-- Data for Name: cms_contents; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.cms_contents (id, content_type, created_at, metadata, updated_at, basic_info_id, description, is_active, section_key, sort_order, title) FROM stdin;
80a3e7b9-a0aa-44c3-8032-39b5dc1a46f7	skills	2026-02-23 01:14:00.854	{"url": "https://www.w3.org/html/", "name": "HTML5", "level": "Expert", "years": "10+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/html5/html5-original-wordmark.svg"}	2026-02-23 01:14:00.854	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
5d11bb0d-102c-48b8-a2bf-c43ba197f1e5	skills	2026-02-23 01:47:40.175	{"url": "https://www.w3schools.com/css/", "name": "CSS3", "level": "Expert", "years": "10+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/css3/css3-original-wordmark.svg"}	2026-02-23 01:47:40.175	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
0c830ce6-b927-40fb-afc6-0644efe136df	skills	2026-02-23 01:48:58.294	{"url": "https://getbootstrap.com", "name": "Bootstrap", "level": "Expert", "years": "8+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/bootstrap/bootstrap-original.svg"}	2026-02-23 01:48:58.294	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
4bee4ce5-b09b-4b81-93db-9fdd463b4c18	skills	2026-02-23 01:49:26.627	{"url": "https://sass-lang.com", "name": "Sass", "level": "Advanced", "years": "6+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/sass/sass-original.svg"}	2026-02-23 01:49:26.627	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
3488ee17-686b-48b4-a575-6d4a16351f42	skills	2026-02-23 01:49:56.495	{"url": "https://developer.mozilla.org/en-US/docs/Web/JavaScript", "name": "JavaScript", "level": "Expert", "years": "10+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/javascript/javascript-original.svg"}	2026-02-23 01:49:56.495	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
5733d2ce-6e92-4b01-8ed8-d69bc55fb40a	skills	2026-02-23 01:50:26.084	{"url": "https://reactjs.org/", "name": "React", "level": "Expert", "years": "8+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/react/react-original-wordmark.svg"}	2026-02-23 01:50:26.084	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
4e2fc977-f158-4f89-9c22-958b73646b35	skills	2026-02-23 01:50:55.828	{"url": "https://code.visualstudio.com/", "name": "VS Code", "level": "Expert", "years": "8+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/vscode/vscode-original.svg"}	2026-02-23 01:50:55.828	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
c598447b-19d2-4f93-8b4f-73205e47bbfb	skills	2026-02-23 01:51:21.217	{"url": "https://www.figma.com/", "name": "Figma", "level": "Expert", "years": "5+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/figma/figma-original.svg"}	2026-02-23 01:51:21.217	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
5cc9bbae-da3c-4935-8801-e99f2c406503	skills	2026-02-23 01:51:47.153	{"url": "https://redux.js.org", "name": "Redux", "level": "Expert", "years": "6+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/redux/redux-original.svg"}	2026-02-23 01:51:47.153	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
0712059a-6e96-4de4-84e1-0de65efaf97c	skills	2026-02-23 01:52:16.078	{"url": "https://nextjs.org/", "name": "Next.js", "level": "Expert", "years": "5+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/nextjs/nextjs-original.svg"}	2026-02-23 01:52:16.078	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
9a607665-ae2d-4417-bc7f-a223f75f407b	skills	2026-02-23 01:52:41.691	{"url": "https://trello.com/en", "name": "Trello", "level": "Advanced", "years": "6+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/trello/trello-original.svg"}	2026-02-23 01:52:41.691	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
1c7c19fc-c0af-4361-8d0c-b66c336674a0	skills	2026-02-23 01:53:13.671	{"url": "https://git-scm.com/", "name": "Git", "level": "Expert", "years": "10+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/git/git-original.svg"}	2026-02-23 01:53:13.671	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
02fa6f48-b88d-4c6f-b546-60d048fb7de7	skills	2026-02-23 01:53:43.839	{"url": "https://nodejs.org", "name": "Node.js", "level": "Advanced", "years": "7+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/nodejs/nodejs-original-wordmark.svg"}	2026-02-23 01:53:43.839	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
b4c65628-61da-4078-abb0-ae2559862f8b	skills	2026-02-23 01:54:08.803	{"url": "https://www.docker.com/", "name": "Docker", "level": "Intermediate", "years": "3+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/docker/docker-original-wordmark.svg"}	2026-02-23 01:54:08.803	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
833d30eb-6926-4b8e-b518-ac03331c19a3	skills	2026-02-23 01:54:45.881	{"url": "https://expressjs.com", "name": "Express", "level": "Advanced", "years": "5+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/express/express-original-wordmark.svg"}	2026-02-23 01:54:45.881	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
b24d2883-4f7e-4826-b49e-724f57b7058c	skills	2026-02-23 01:55:20.659	{"url": "https://www.prisma.io/", "name": "Prisma", "level": "Intermediate", "years": "3+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/prisma/prisma-original.svg"}	2026-02-23 01:55:20.659	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
ecbf54c5-8ee4-4da1-a72e-09b12d733a9b	skills	2026-02-23 01:55:57.321	{"url": "https://www.mongodb.com/", "name": "MongoDB", "level": "Advanced", "years": "5+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/mongodb/mongodb-original-wordmark.svg"}	2026-02-23 01:55:57.321	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
f3c05424-17df-4f11-ae6c-f7fc0a4240d1	skills	2026-02-23 01:56:25.363	{"url": "https://www.nginx.com", "name": "Nginx", "level": "Intermediate", "years": "3+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/nginx/nginx-original.svg"}	2026-02-23 01:56:25.363	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
bdf90dd1-634a-4644-bc9d-31c60f61e6c7	skills	2026-02-23 01:56:50.664	{"url": "https://postman.com", "name": "Postman", "level": "Advanced", "years": "5+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/postman/postman-original.svg"}	2026-02-23 01:56:50.664	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
29b1a626-2604-4add-8799-de0027ae27e0	skills	2026-02-23 01:57:17.16	{"url": "https://www.postgresql.org", "name": "PostgreSQL", "level": "Intermediate", "years": "3+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/postgresql/postgresql-original-wordmark.svg"}	2026-02-23 01:57:17.16	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
0d0737fd-6600-4ece-a072-fe2cfb466b71	skills	2026-02-23 01:57:49.985	{"url": "https://www.oracle.com/java/", "name": "Java", "level": "Intermediate", "years": "2+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg"}	2026-02-23 01:57:49.985	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
8368cf60-8e19-4248-b690-6eb7a47ae492	skills	2026-02-23 01:58:17.328	{"url": "https://spring.io/", "name": "Spring", "level": "Intermediate", "years": "2+", "imageUrl": "https://raw.githubusercontent.com/devicons/devicon/master/icons/spring/spring-original.svg"}	2026-02-23 01:58:17.328	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
1ebc9158-ad06-4a95-893d-156b77b2fdd3	skills	2026-02-23 01:58:53.514	{"url": "https://www.mcp.com/", "name": "MCP", "level": "Intermediate", "years": "1+", "imageUrl": "https://avatars.githubusercontent.com/u/182288589?s=200&v=4"}	2026-02-23 01:58:53.514	97608fe6-a3eb-4111-8972-e48809dd272f	\N	\N	\N	\N	\N
ede7ab7e-62c2-4643-b909-f6b422a4e75a	experience	2026-02-23 03:06:23.561	{"period": "Mayıs 2023 - Ekim 2023", "company": "Venhancer", "industry": "Fintech", "location": "İstanbul", "position": "Sr. Frontend Developer", "sortOrder": 0, "description": "Fibabanka’nın Yaklaşan Ödemeler uygulaması, Müşteri kampanya yönetim ekranı ve şikayet yönetim ekranı geliştirmelerinde yer aldım.", "achievements": ["Müşteri kampanya yönetim ekranı geliştirme", "Şikayet yönetim ekranı geliştirme", "Yaklaşan Ödemeler uygulaması geliştirme"], "technologies": ["React", "Redux", "TypeScript", "SCSS", "Context API"]}	2026-02-23 03:06:23.561	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
9d330362-f8ec-40dd-8e75-385d436ff26b	experience	2026-02-23 03:06:23.562	{"period": "Mart 2022 - Mayıs 2023", "company": "Azerion Turkey", "industry": "Media", "location": "İstanbul", "position": "Full Stack Developer", "sortOrder": 0, "description": "Kullanıcıların oyun oynayabildiği, oyunlara puan verebildiği, yorum yapabildiği ve gerçek zamanlı sohbetle iletişim kurabildiği çok oyunculu bir oyun platformu ve medya gruplarına ait whitelabel oyun portalları geliştirmelerinde yer aldım.", "achievements": ["Medya gruplarına ait oyun portalları geliştirme", "SEO optimizasyonları", "Performans optimizasyonları, Code splitting, Lazy loading, Image optimization, Bundling optimization..."], "technologies": ["JavaScript", "jQuery", "Bootstrap", "React", "Redux", "TypeScript", "SCSS", "Context API", "GraphQL", "Express", "Socket.io", "Docker", "Jenkins", "CI/CD"]}	2026-02-23 03:06:23.562	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
49283455-9f30-4c55-b85b-542f8ee47964	experience	2026-02-23 03:06:23.563	{"period": "Kasım 2020 - Mart 2022", "company": "Defacto", "industry": "E-commerce", "location": "İstanbul", "position": "Sr. Frontend Developer", "sortOrder": 0, "description": "Türkiye’nin en büyük e-ticaret platformlarından birisi olan Defacto’nun frontend geliştirme mimarısınde yer aldım.", "achievements": ["Frontend mimarisinde mobile ve desktop uygulamasını responsive olarak yönetilmesi", "SEO, Analytics ve Datalayer entegrasyonları", "Performans optimizasyonları (Code splitting, Lazy loading, Image optimization, Bundling optimization...)"], "technologies": ["HTML", "CSS", "JavaScript", "TypeScript", "React", "Redux", "SCSS", "Context API", "Angular V6", "Webpack"]}	2026-02-23 03:06:23.563	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
bc57f8ca-2be1-4647-8c20-74aa12d4e6c0	experience	2026-02-23 03:06:23.563	{"period": "Nisan 2019 - Ekim 2020", "company": "Nuevo Softwarehouse", "industry": "Agency", "location": "İstanbul", "position": "Frontend Developer", "sortOrder": 0, "description": "Nuevo Softwarehouse’un frontend geliştirme ekibine katıldım. Junior developer mentorluğu yapıyordum. TCCB, OTİ, ATASUN ve BITTRT gibi büyük ölçekli firmaların frontend geliştirmelerinde yer aldım.", "achievements": ["Junior developer mentorluğu", "Performans optimizasyonları (Code splitting, Lazy loading, Image optimization, Bundling optimization...)"], "technologies": ["React", "Redux", "TypeScript", "SCSS", "Context API", "Angular V6", "Webpack", "Gulp"]}	2026-02-23 03:06:23.563	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
878fd7e5-e03a-435f-9e6c-ef7eb9978cfc	experience	2026-02-23 03:06:23.563	{"period": "Temmuz 2017 - Nisan 2019", "company": "Freelancer", "industry": "Freelancer", "location": "İstanbul", "position": "Full Stack Developer", "sortOrder": 0, "description": "Freelancer olarak çeşitli firmaların sistemlerinde frontend ve backend geliştirmelerinde yer aldım. Intranet projeler olduğundan panel geliştirmeleri, önyüz geliştirmeleri, Rest API ve entegrasyonları gibi geliştirmeler yapıyordum.", "achievements": ["Bir çok firmanın sistemlerinde frontend ve backend geliştirmelerinde yer aldım.", "Rest API ve entegrasyonları gibi geliştirmeleri.", "Intranet projelerinde panel geliştirmeleri."], "technologies": ["JavaScript", "jQuery", "Bootstrap", "SCSS", "PHP", "Twig", "Codeigniter", "mysql"]}	2026-02-23 03:06:23.563	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
c7d5837e-bdba-4404-a1b7-d24fdc20cc48	experience	2026-02-23 03:06:23.564	{"period": "Aralık 2011 - Temmuz 2017", "company": "Projesoft", "industry": "E-commerce", "location": "İstanbul", "position": "Frontend Developer", "sortOrder": 0, "description": "Projesoft’un frontend geliştirme ekibine katıldım. Bir çok firmanın E-Ticaret sistemlerinin frontend geliştirmelerinde yer aldım. bunlardan bazıları: kitapisler.com, kuyumcu.com.tr, feyioglu.com.tr, network.com.tr, divarese.com, beymen.com gibi.", "achievements": ["Bir çok firmanın E-Ticaret sistemlerinin frontend geliştirmelerinde yer aldım."], "technologies": ["JavaScript", "jQuery", "Bootstrap", "SCSS", "PHP", "Twig"]}	2026-02-23 03:06:23.564	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
96a9d06f-bd3e-4d14-bca5-5788d1306c6c	experience	2026-02-23 03:06:23.509	{"period": "Ekim 2023 - Günümüz", "company": "Hangikredi", "industry": "Fintech", "location": "İstanbul", "position": "Senior Frontend Developer & Team Lead", "sortOrder": 0, "description": "Türkiye’nin en büyük karşılaştırmalı finansal ürünler sunan şirketi. React ve Next.js kullanarak fintech uygulamaları geliştiriyorum. bulunduğum squadın developer ekibine liderlik ediyorum.", "achievements": ["40% performans artışı sağlayan optimizasyonlar", "NPM paketi kurulumu", "SEO, Analytics ve Datalayer entegrasyonları", "CI/CD Jenkins ve Docker kullanarak pipeline kurulumu", "Junior developer mentorluğu"], "technologies": ["React", "Next.js", "React Query", "TypeScript", "C# .Net", "MVC Razor", "SCSS", "Context API", "SignalR", "Tailwind CSS", "CSR & SSR Rendering", "Docker", "Jenkins", "CI/CD", "Gulp"]}	2026-02-23 21:59:25.679	fc90b992-dc1a-402e-8465-ead5fcc64b68	\N	\N	\N	\N	\N
\.


--
-- Data for Name: comments; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.comments (id, created_at, updated_at, content, email, name, status, parent_comment_id, post_id) FROM stdin;
\.


--
-- Data for Name: component_banners; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.component_banners (component_id, banner_id) FROM stdin;
2	1
2	2
\.


--
-- Data for Name: component_forms; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.component_forms (component_id, form_definition_id) FROM stdin;
\.


--
-- Data for Name: component_widgets; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.component_widgets (component_id, widget_id) FROM stdin;
1	1
3	2
\.


--
-- Data for Name: components; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.components (id, created_at, updated_at, content, description, name, order_index, status, template, type) FROM stdin;
2	2026-01-25 01:26:14.061	2026-01-25 02:45:37.815		Anasayfa Vitrin 2 Açıklama	Resimli Component	2	t	BComponent	0
1	2026-01-25 01:13:04.212	2026-01-25 02:46:22.15	gelismis icerk	Anasayfa Vitrin 1 Açıklama	Widgetli Component	1	t	AComponent	1
3	2026-01-25 01:28:37.802	2026-01-25 02:49:54.886		Anasayfa Vitrin 3 Açıklama	Widgetli Component	3	t	CComponent	1
\.


--
-- Data for Name: email_logs; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.email_logs (id, created_at, updated_at, error_message, payload_json, recipient, retry_count, sent_at, status, subject, template_name) FROM stdin;
1	2026-02-15 21:37:51.766	2026-02-15 21:37:52.463	\N	{"name":"Test User","message":"Merhaba!","actionUrl":"https://example.com","actionText":"Başlayın"}	test@example.com	0	2026-02-15 21:37:52.456	SENT	Hoş Geldiniz!	welcome
\.


--
-- Data for Name: form_definitions; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.form_definitions (id, created_at, updated_at, active, schema, title, version) FROM stdin;
2	2026-02-14 00:57:44.945	2026-02-15 02:49:36.671	t	{"steps": [{"id": "7197e941-e6b0-4948-b299-4a53e4dcce76", "title": "adim 1", "description": "adim 1 a"}, {"id": "e9e60343-c880-40a7-901d-f8f0c803bce9", "title": "adim 2", "description": "adim 2 as"}], "config": {"layout": "wizard", "submitLabel": "Gönder butonu"}, "fields": [{"id": "field_1771019704431", "type": "text", "label": "film adi", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": null, "required": true, "condition": null, "validation": null}, {"id": "field_1771019724218", "type": "number", "label": "film puan", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": null, "required": false, "condition": null, "validation": null}, {"id": "yorum_ekle", "type": "checkbox", "label": "Yorum girmek istiyorum", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": null, "required": false, "condition": null, "validation": null}, {"id": "field_1771019796965", "type": "textarea", "label": "yorum", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": null, "required": false, "condition": {"field": "yorum_ekle", "value": "true", "operator": "EQUALS"}, "validation": null}, {"id": "begeni_derecesi", "type": "radio", "label": "Beğeni Derecesi", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": [{"label": "az", "value": "az"}, {"label": "orta", "value": "orta"}, {"label": "cok", "value": "cok"}], "required": false, "condition": null, "validation": null}, {"id": "turu", "type": "select", "label": "film türü", "stepId": "7197e941-e6b0-4948-b299-4a53e4dcce76", "options": [{"label": "drama", "value": "drama"}, {"label": "bilimkurgu", "value": "bilimkurgu"}], "required": false, "condition": null, "validation": null}, {"id": "multi_check", "type": "multi_checkbox", "label": "çoklu seçim", "stepId": "e9e60343-c880-40a7-901d-f8f0c803bce9", "options": [{"label": "elma", "value": "elma"}, {"label": "armut", "value": "armut"}, {"label": "muz", "value": "muz"}], "required": false, "condition": null, "validation": null}, {"id": "ikici_adim", "type": "separator", "label": "asdsad", "stepId": "e9e60343-c880-40a7-901d-f8f0c803bce9", "options": null, "required": false, "condition": null, "validation": null}]}	film degerlendir	11
\.


--
-- Data for Name: form_submissions; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.form_submissions (id, created_at, updated_at, payload, submitted_at, form_definition_id) FROM stdin;
1	2026-02-14 02:05:50.098	2026-02-14 02:05:50.098	{"field_1771019704431": "ironman", "field_1771019724218": 5, "field_1771019740020": true, "field_1771019796965": "mukemmel bir bilim kurgu"}	2026-02-14 02:05:50.117319	2
2	2026-02-15 02:51:25.241	2026-02-15 02:51:25.241	{"turu": "", "yorum_ekle": false, "multi_check": "", "begeni_derecesi": "", "field_1771019704431": "qwe", "field_1771019724218": 222}	2026-02-15 02:51:25.295806	2
3	2026-02-15 02:51:42.005	2026-02-15 02:51:42.005	{"turu": "", "yorum_ekle": false, "multi_check": "", "begeni_derecesi": "", "field_1771019704431": "qwe", "field_1771019724218": 2}	2026-02-15 02:51:42.006403	2
4	2026-02-15 02:53:07.899	2026-02-15 02:53:07.899	{"turu": "", "ikici_adim": "", "yorum_ekle": false, "multi_check": "", "begeni_derecesi": "", "field_1771019704431": "qwe", "field_1771019724218": 2}	2026-02-15 02:53:07.902902	2
\.


--
-- Data for Name: page_components; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.page_components (page_id, component_id) FROM stdin;
1	1
1	2
1	3
\.


--
-- Data for Name: pages; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.pages (id, created_at, updated_at, description, slug, status, template, title, seo_info_id) FROM stdin;
1	2026-01-25 01:12:26.115	2026-01-25 01:29:57.883	anasayfa açıklama 	home	t	APage	Ana sayfa	1
\.


--
-- Data for Name: posts; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.posts (id, created_at, updated_at, content, order_index, slug, status, template, title, seo_info_id) FROM stdin;
1	2026-01-25 01:20:28.795	2026-01-25 01:20:28.795	deemee post icerik\ndeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerik\ndeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerik\ndeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerik\ndeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerik\ndeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerikdeemee post icerik	0	deneme-post	t	APost	deneme post	2
\.


--
-- Data for Name: ratings; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.ratings (id, created_at, updated_at, comment, rating, user_identifier, post_id) FROM stdin;
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.refresh_tokens (id, created_at, updated_at, expiry_date, is_revoked, token, user_id) FROM stdin;
53	2026-02-15 03:30:20.693	2026-02-15 03:30:20.693	2026-02-15 04:30:20.692	f	eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..0kddbRu3S5KcHEod.ruUzElzJ3S36uNxk-0dvNficcZ8LpdU_haUnDQIAmJg_T0TS7rlrz_ehejIVpHH5XDr1NJFAVVofILpPHjqmnv3V466O1QeC1GXrkTk.nElGVw5Y0getbdHlMa0brQ	2
61	2026-02-23 03:06:49.343	2026-03-01 21:16:13.162118	2026-03-01 22:16:13.618	f	eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..4JTP7eVOCNvEl1Wn._9WxQLTpcwKmwxxJUOV5AvISqo2AbwVCIFBGUKBLKqGMSi2vk6sqAodLdNV5ZdBk0lA4rOjrlyBL_NKFlhwP7hmBaQ3NfGPQuhnRriQ4a3X3Nm919PTFCefHABjaV23PB9QuWkMUNQ.l79RyZHdPmNqBeuHlLxBAA	1
\.


--
-- Data for Name: seo_infos; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.seo_infos (id, created_at, updated_at, canonical_url, description, keywords, no_follow, no_index, title) FROM stdin;
1	2026-01-25 01:12:26.153	2026-01-25 01:12:26.153	https://www.huseyindol.site/ana-sayfa	Ana Sayfa SEO Meta Açıklama	Ana Sayfa SEO Meta Açıklama,Ana Sayfa SEO Meta Açıklama,Ana Sayfa SEO Meta Açıklama	f	f	Ana Sayfa SEO Başlığı
2	2026-01-25 01:20:28.804	2026-01-25 01:20:28.804	https://www.huseyindol.site/deneme-post	deneme post aciklama	deneme, post, aciklama	f	f	deneme post
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.users (id, created_at, updated_at, email, first_name, is_active, last_name, password, provider, provider_id, token_version, username) FROM stdin;
2	2026-01-22 22:31:29.431	2026-02-15 03:30:20.627	admin@admin.com	admin	t	admin	$2a$10$zgmeBlJarB9sf329cAukm.38dhdfDMViCPVBJIiH91my9FaS/AsBS	local	\N	4	admin
1	2026-01-22 22:30:54.209	2026-03-01 21:16:13.162118	huseyindol@gmail.com	huseyin	t	dol	$2a$10$B98ka1J4qW.nnWKs1X5ilu9/pLcrC86DINjLYm/vulqjQdPyjp0AC	local	\N	59	huseyindol
\.


--
-- Data for Name: widget_banners; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.widget_banners (widget_id, banner_id) FROM stdin;
2	1
2	2
\.


--
-- Data for Name: widget_posts; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.widget_posts (widget_id, post_id) FROM stdin;
1	1
\.


--
-- Data for Name: widgets; Type: TABLE DATA; Schema: elly; Owner: postgres
--

COPY elly.widgets (id, created_at, updated_at, content, description, name, order_index, status, template, type) FROM stdin;
2	2026-01-25 01:29:07.112	2026-01-25 01:29:07.112		Resimli Widget Açıklama	Resimli Widget	2	t	BWidget	0
1	2026-01-25 01:14:15.684	2026-01-25 01:29:33.262		Postlu Widget Açıklama	Postlu Widget	1	t	AWidget	1
\.


--
-- Name: assets_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.assets_id_seq', 28, true);


--
-- Name: banners_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.banners_id_seq', 2, true);


--
-- Name: comments_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.comments_id_seq', 1, false);


--
-- Name: components_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.components_id_seq', 3, true);


--
-- Name: email_logs_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.email_logs_id_seq', 1, true);


--
-- Name: form_definitions_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.form_definitions_id_seq', 2, true);


--
-- Name: form_submissions_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.form_submissions_id_seq', 4, true);


--
-- Name: pages_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.pages_id_seq', 1, true);


--
-- Name: posts_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.posts_id_seq', 1, true);


--
-- Name: ratings_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.ratings_id_seq', 1, false);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.refresh_tokens_id_seq', 63, true);


--
-- Name: seo_infos_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.seo_infos_id_seq', 2, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.users_id_seq', 2, true);


--
-- Name: widgets_id_seq; Type: SEQUENCE SET; Schema: elly; Owner: postgres
--

SELECT pg_catalog.setval('elly.widgets_id_seq', 2, true);


--
-- Name: assets assets_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.assets
    ADD CONSTRAINT assets_pkey PRIMARY KEY (id);


--
-- Name: banners banners_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.banners
    ADD CONSTRAINT banners_pkey PRIMARY KEY (id);


--
-- Name: cms_basic_infos cms_basic_infos_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.cms_basic_infos
    ADD CONSTRAINT cms_basic_infos_pkey PRIMARY KEY (id);


--
-- Name: cms_contents cms_contents_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.cms_contents
    ADD CONSTRAINT cms_contents_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: component_banners component_banners_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_banners
    ADD CONSTRAINT component_banners_pkey PRIMARY KEY (component_id, banner_id);


--
-- Name: component_forms component_forms_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_forms
    ADD CONSTRAINT component_forms_pkey PRIMARY KEY (component_id, form_definition_id);


--
-- Name: component_widgets component_widgets_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_widgets
    ADD CONSTRAINT component_widgets_pkey PRIMARY KEY (component_id, widget_id);


--
-- Name: components components_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.components
    ADD CONSTRAINT components_pkey PRIMARY KEY (id);


--
-- Name: email_logs email_logs_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.email_logs
    ADD CONSTRAINT email_logs_pkey PRIMARY KEY (id);


--
-- Name: form_definitions form_definitions_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.form_definitions
    ADD CONSTRAINT form_definitions_pkey PRIMARY KEY (id);


--
-- Name: form_submissions form_submissions_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.form_submissions
    ADD CONSTRAINT form_submissions_pkey PRIMARY KEY (id);


--
-- Name: pages id_page_page_slug; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.pages
    ADD CONSTRAINT id_page_page_slug UNIQUE (slug);


--
-- Name: posts idx_post_slug; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.posts
    ADD CONSTRAINT idx_post_slug UNIQUE (slug);


--
-- Name: refresh_tokens idx_refresh_token_token; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.refresh_tokens
    ADD CONSTRAINT idx_refresh_token_token UNIQUE (token);


--
-- Name: users idx_user_email; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.users
    ADD CONSTRAINT idx_user_email UNIQUE (email);


--
-- Name: users idx_user_username; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.users
    ADD CONSTRAINT idx_user_username UNIQUE (username);


--
-- Name: page_components page_components_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.page_components
    ADD CONSTRAINT page_components_pkey PRIMARY KEY (page_id, component_id);


--
-- Name: pages pages_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.pages
    ADD CONSTRAINT pages_pkey PRIMARY KEY (id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: ratings ratings_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.ratings
    ADD CONSTRAINT ratings_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: seo_infos seo_infos_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.seo_infos
    ADD CONSTRAINT seo_infos_pkey PRIMARY KEY (id);


--
-- Name: pages uc_page_slug; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.pages
    ADD CONSTRAINT uc_page_slug UNIQUE (slug);


--
-- Name: posts uc_post_slug; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.posts
    ADD CONSTRAINT uc_post_slug UNIQUE (slug);


--
-- Name: ratings uc_rating_user_post; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.ratings
    ADD CONSTRAINT uc_rating_user_post UNIQUE (user_identifier, post_id);


--
-- Name: users uc_user_email; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.users
    ADD CONSTRAINT uc_user_email UNIQUE (email);


--
-- Name: users uc_user_username; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.users
    ADD CONSTRAINT uc_user_username UNIQUE (username);


--
-- Name: refresh_tokens uk7tdcd6ab5wsgoudnvj7xf1b7l; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.refresh_tokens
    ADD CONSTRAINT uk7tdcd6ab5wsgoudnvj7xf1b7l UNIQUE (user_id);


--
-- Name: assets ukln4kj492rlnemwox5b7x38ld7; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.assets
    ADD CONSTRAINT ukln4kj492rlnemwox5b7x38ld7 UNIQUE (name, sub_folder);


--
-- Name: posts ukml7eiucwpy3gkvs30195gvmdl; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.posts
    ADD CONSTRAINT ukml7eiucwpy3gkvs30195gvmdl UNIQUE (seo_info_id);


--
-- Name: pages ukop7mbivq89lx29uoeta2o6hc1; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.pages
    ADD CONSTRAINT ukop7mbivq89lx29uoeta2o6hc1 UNIQUE (seo_info_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: widget_banners widget_banners_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_banners
    ADD CONSTRAINT widget_banners_pkey PRIMARY KEY (widget_id, banner_id);


--
-- Name: widget_posts widget_posts_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_posts
    ADD CONSTRAINT widget_posts_pkey PRIMARY KEY (widget_id, post_id);


--
-- Name: widgets widgets_pkey; Type: CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widgets
    ADD CONSTRAINT widgets_pkey PRIMARY KEY (id);


--
-- Name: id_banner_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_banner_status ON elly.banners USING btree (status);


--
-- Name: id_banner_title; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_banner_title ON elly.banners USING btree (title);


--
-- Name: id_component_name; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_component_name ON elly.components USING btree (name);


--
-- Name: id_component_type; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_component_type ON elly.components USING btree (type);


--
-- Name: id_component_type_name; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_component_type_name ON elly.components USING btree (type, name);


--
-- Name: id_component_type_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_component_type_status ON elly.components USING btree (type, status);


--
-- Name: id_page_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_page_status ON elly.pages USING btree (status);


--
-- Name: id_seoinfo_canonical_url; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_seoinfo_canonical_url ON elly.seo_infos USING btree (canonical_url);


--
-- Name: id_seoinfo_title; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_seoinfo_title ON elly.seo_infos USING btree (title);


--
-- Name: id_widget_name; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_widget_name ON elly.widgets USING btree (name);


--
-- Name: id_widget_type; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_widget_type ON elly.widgets USING btree (type);


--
-- Name: id_widget_type_name; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_widget_type_name ON elly.widgets USING btree (type, name);


--
-- Name: id_widget_type_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX id_widget_type_status ON elly.widgets USING btree (type, status);


--
-- Name: idx_cms_basic_info_active; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_cms_basic_info_active ON elly.cms_basic_infos USING btree (is_active);


--
-- Name: idx_cms_basic_info_section_key; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_cms_basic_info_section_key ON elly.cms_basic_infos USING btree (section_key);


--
-- Name: idx_cms_content_active; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_cms_content_active ON elly.cms_contents USING btree (is_active);


--
-- Name: idx_cms_content_content_type; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_cms_content_content_type ON elly.cms_contents USING btree (content_type);


--
-- Name: idx_cms_content_section_key; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_cms_content_section_key ON elly.cms_contents USING btree (section_key);


--
-- Name: idx_comment_parent_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_comment_parent_id ON elly.comments USING btree (parent_comment_id);


--
-- Name: idx_comment_post_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_comment_post_id ON elly.comments USING btree (post_id);


--
-- Name: idx_comment_post_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_comment_post_status ON elly.comments USING btree (post_id, status);


--
-- Name: idx_comment_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_comment_status ON elly.comments USING btree (status);


--
-- Name: idx_email_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_email_status ON elly.email_logs USING btree (status);


--
-- Name: idx_email_status_created; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_email_status_created ON elly.email_logs USING btree (status, created_at);


--
-- Name: idx_form_def_active; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_form_def_active ON elly.form_definitions USING btree (active);


--
-- Name: idx_form_def_title; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_form_def_title ON elly.form_definitions USING btree (title);


--
-- Name: idx_form_sub_form_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_form_sub_form_id ON elly.form_submissions USING btree (form_definition_id);


--
-- Name: idx_form_sub_submitted_at; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_form_sub_submitted_at ON elly.form_submissions USING btree (submitted_at);


--
-- Name: idx_post_seo_info_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_post_seo_info_id ON elly.posts USING btree (seo_info_id);


--
-- Name: idx_post_status; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_post_status ON elly.posts USING btree (status);


--
-- Name: idx_rating_post_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_rating_post_id ON elly.ratings USING btree (post_id);


--
-- Name: idx_rating_user_post; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_rating_user_post ON elly.ratings USING btree (user_identifier, post_id);


--
-- Name: idx_refresh_token_user_id; Type: INDEX; Schema: elly; Owner: postgres
--

CREATE INDEX idx_refresh_token_user_id ON elly.refresh_tokens USING btree (user_id);


--
-- Name: refresh_tokens fk1lih5y2npsf8u5o3vhdb9y0os; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES elly.users(id);


--
-- Name: form_submissions fk2kxl75gxcdqrjh4n9k5qo27q8; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.form_submissions
    ADD CONSTRAINT fk2kxl75gxcdqrjh4n9k5qo27q8 FOREIGN KEY (form_definition_id) REFERENCES elly.form_definitions(id);


--
-- Name: widget_posts fk3ssrfk5po43ldy0rtd0jmx1a8; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_posts
    ADD CONSTRAINT fk3ssrfk5po43ldy0rtd0jmx1a8 FOREIGN KEY (post_id) REFERENCES elly.posts(id);


--
-- Name: component_banners fk5pb7bbbtpn4722qh2d75mlpnx; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_banners
    ADD CONSTRAINT fk5pb7bbbtpn4722qh2d75mlpnx FOREIGN KEY (banner_id) REFERENCES elly.banners(id);


--
-- Name: comments fk7h839m3lkvhbyv3bcdv7sm4fj; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.comments
    ADD CONSTRAINT fk7h839m3lkvhbyv3bcdv7sm4fj FOREIGN KEY (parent_comment_id) REFERENCES elly.comments(id);


--
-- Name: widget_posts fka8ax06lro5qx37mv5yo08l1ck; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_posts
    ADD CONSTRAINT fka8ax06lro5qx37mv5yo08l1ck FOREIGN KEY (widget_id) REFERENCES elly.widgets(id);


--
-- Name: posts fkb7fka510air19xqvbxtod9ht0; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.posts
    ADD CONSTRAINT fkb7fka510air19xqvbxtod9ht0 FOREIGN KEY (seo_info_id) REFERENCES elly.seo_infos(id);


--
-- Name: component_widgets fkc2i9net1t9vlm4dmbcm5mlcsj; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_widgets
    ADD CONSTRAINT fkc2i9net1t9vlm4dmbcm5mlcsj FOREIGN KEY (component_id) REFERENCES elly.components(id);


--
-- Name: component_widgets fkcenu7fw3b2eo8pv0oo48hqunc; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_widgets
    ADD CONSTRAINT fkcenu7fw3b2eo8pv0oo48hqunc FOREIGN KEY (widget_id) REFERENCES elly.widgets(id);


--
-- Name: component_banners fkcnj5y54vcky4w0ww2gvt87y7g; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_banners
    ADD CONSTRAINT fkcnj5y54vcky4w0ww2gvt87y7g FOREIGN KEY (component_id) REFERENCES elly.components(id);


--
-- Name: page_components fkdfob1d9oq7vfmb8o67n27vohg; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.page_components
    ADD CONSTRAINT fkdfob1d9oq7vfmb8o67n27vohg FOREIGN KEY (component_id) REFERENCES elly.components(id);


--
-- Name: comments fkh4c7lvsc298whoyd4w9ta25cr; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.comments
    ADD CONSTRAINT fkh4c7lvsc298whoyd4w9ta25cr FOREIGN KEY (post_id) REFERENCES elly.posts(id);


--
-- Name: page_components fkk0cqx20neichlg97egj3jgqg0; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.page_components
    ADD CONSTRAINT fkk0cqx20neichlg97egj3jgqg0 FOREIGN KEY (page_id) REFERENCES elly.pages(id);


--
-- Name: ratings fkl38egjrojmh8hq3uoyo7is8h6; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.ratings
    ADD CONSTRAINT fkl38egjrojmh8hq3uoyo7is8h6 FOREIGN KEY (post_id) REFERENCES elly.posts(id);


--
-- Name: pages fkl9l1wjycqrl62d4r8jn3u4nu5; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.pages
    ADD CONSTRAINT fkl9l1wjycqrl62d4r8jn3u4nu5 FOREIGN KEY (seo_info_id) REFERENCES elly.seo_infos(id);


--
-- Name: widget_banners fklsci7uxy4fqqj23t52huyis42; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_banners
    ADD CONSTRAINT fklsci7uxy4fqqj23t52huyis42 FOREIGN KEY (widget_id) REFERENCES elly.widgets(id);


--
-- Name: component_forms fknlk3jjp9ybylq5ra6x1ygjj8a; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_forms
    ADD CONSTRAINT fknlk3jjp9ybylq5ra6x1ygjj8a FOREIGN KEY (component_id) REFERENCES elly.components(id);


--
-- Name: cms_contents fko0xv9diurwi1ltwbceg7t694c; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.cms_contents
    ADD CONSTRAINT fko0xv9diurwi1ltwbceg7t694c FOREIGN KEY (basic_info_id) REFERENCES elly.cms_basic_infos(id);


--
-- Name: widget_banners fkqp8ibilv4fv8pywrgn1h847op; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.widget_banners
    ADD CONSTRAINT fkqp8ibilv4fv8pywrgn1h847op FOREIGN KEY (banner_id) REFERENCES elly.banners(id);


--
-- Name: component_forms fkr7eas7bmrp0oy59xx18r2btgv; Type: FK CONSTRAINT; Schema: elly; Owner: postgres
--

ALTER TABLE ONLY elly.component_forms
    ADD CONSTRAINT fkr7eas7bmrp0oy59xx18r2btgv FOREIGN KEY (form_definition_id) REFERENCES elly.form_definitions(id);


--
-- PostgreSQL database dump complete
--

\unrestrict I3ibXuejYVZ92yoNDahtxTuC5hrxaGHZejuIneG25EuWvdJ07CByaBJNLGxH2xb

