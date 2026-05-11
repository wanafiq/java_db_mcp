# java-db-mcp

A Java Spring Boot [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) server that gives AI assistants like Claude direct access to databases. It exposes query execution and schema discovery as MCP tools and resources.

Built with Spring Boot 4, Spring AI, and STDIO transport -- designed so each project can configure its own database connection and permissions. Currently supports **MySQL**, **PostgreSQL**, **MongoDB**, **Redis**, and **Elasticsearch**.

One jar bundles all drivers. Pick the database per process via the `DB_TYPE` env var. Run the jar multiple times in your MCP client config to talk to several databases at once.

## Features

- **SQL**: `sql_query` tool, `db://tables` + `db://tables/{name}` resources, permission controls over SELECT/INSERT/UPDATE/DELETE/DDL.
- **MongoDB**: `mongo_find`, `mongo_aggregate`, `mongo_insert`, `mongo_update`, `mongo_delete` tools, `db://collections` + `db://collections/{name}` resources, permission controls over WRITE/DELETE/ADMIN.
- **Redis**: `redis_command` tool dispatching arbitrary commands with default-deny classification, `db://keys` + `db://keys/{key}` resources, permission controls over WRITE/ADMIN.
- **Elasticsearch**: `es_search`, `es_get`, `es_index`, `es_update`, `es_delete` tools, `db://indices` + `db://indices/{name}` resources, permission controls over WRITE/DELETE/ADMIN.
- **Bounded results** -- every row-returning tool is capped by `MAX_ROWS` (default 500). Responses include `truncated: true` when more data is available; use `LIMIT/OFFSET` (SQL), `skip` (Mongo `find`), `$skip` (Mongo `aggregate`), or `from` (ES) to page.
- **Per-project configuration** -- Each project passes its own credentials via environment variables.
- **Read-only by default** -- All write operations require explicit opt-in.
- **SQL parsing** -- [JSqlParser](https://github.com/JSQLParser/JSqlParser) classifies SQL statement types so permission checks can't be bypassed by comments or CTEs.

## Requirements

- Java 25+
- Maven 3.9+
- One of: MySQL 8.0+, PostgreSQL 12+, MongoDB 5.0+, Redis 6.0+, Elasticsearch 8.0+ (or OpenSearch with the ES-compatible REST API)

## Quick Start

### 1. Build

```bash
git clone https://github.com/wanafiq/java_db_mcp.git
cd java_db_mcp
./mvnw package -DskipTests
```

This produces `java-db-mcp.jar` in the project root (also available at `target/java-db-mcp.jar`).

### 2. Configure your AI client

Add the MCP server to your client's configuration. The `env` block is where you specify which database to connect to and what operations are allowed.

#### Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "mysql": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/java-db-mcp.jar"
      ],
      "env": {
        "DB_TYPE": "mysql",
        "DB_HOST": "127.0.0.1",
        "DB_PORT": "3306",
        "DB_USER": "root",
        "DB_PASSWORD": "your_password",
        "DB_NAME": "your_database",
        "SQL_ALLOW_INSERT": "false",
        "SQL_ALLOW_UPDATE": "false",
        "SQL_ALLOW_DELETE": "false",
        "SQL_ALLOW_DDL": "false"
      }
    }
  }
}
```

#### Claude Code

Add to your project's `.mcp.json` or `~/.claude.json`:

```json
{
  "mcpServers": {
    "mysql": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/java-db-mcp.jar"
      ],
      "env": {
        "DB_TYPE": "mysql",
        "DB_HOST": "127.0.0.1",
        "DB_PORT": "3306",
        "DB_USER": "root",
        "DB_PASSWORD": "your_password",
        "DB_NAME": "your_database",
        "SQL_ALLOW_INSERT": "false",
        "SQL_ALLOW_UPDATE": "false",
        "SQL_ALLOW_DELETE": "false",
        "SQL_ALLOW_DDL": "false"
      }
    }
  }
}
```

### 3. Use it

Once connected, the AI assistant can:

- **Discover tables**: reads `db://tables` to see what's in the database
- **Inspect schema**: reads `db://tables/users` to see column definitions
- **Run queries**: calls `sql_query` with SQL like `SELECT * FROM users LIMIT 10`

## Configuration

All configuration is done through environment variables, making it easy to use different databases per project.

### Connection

| Variable | Description | Default |
|---|---|---|
| `DB_TYPE` | Database type: `mysql`, `postgres`, `mongo`, `redis`, or `elasticsearch` | `mysql` |
| `DB_HOST` | Database server hostname | `localhost` |
| `DB_PORT` | Database server port | `3306` mysql / `5432` postgres / `27017` mongo / `6379` redis / `9200` elasticsearch |
| `DB_USER` | Database username | `root` mysql / `postgres` postgres / *(empty)* mongo, redis, es |
| `DB_PASSWORD` | Database password | *(empty)* |
| `DB_NAME` | Database name (**required** for `mongo`; unused for `redis` and `elasticsearch`) | *(empty)* |
| `DB_URI` | Full connection URI (`mongo`, `elasticsearch`) -- overrides host/port/user/password | *(empty)* |
| `REDIS_DB` | Redis logical DB index (`redis` only) | `0` |
| `REDIS_SSL` | Enable TLS for Redis connection (`redis` only) | `false` |
| `REDIS_SSL_INSECURE` | When `REDIS_SSL=true`, also skip certificate + hostname verification. Useful for SSH-tunneled or self-signed setups. | `false` |
| `ES_API_KEY` | Elasticsearch API key (`elasticsearch` only). When set, overrides basic auth. | *(empty)* |
| `MAX_ROWS` | Cap on rows returned per query (applies to `sql_query`, `mongo_find`, `mongo_aggregate`, `es_search`). When exceeded, the response includes `truncated: true`. | `500` |

### Permissions

All write operations are **disabled by default**. Permissions are namespaced per database family so the verbs match each DB's reality.

**SQL (`DB_TYPE=mysql` or `postgres`):**

| Variable | Description | Default |
|---|---|---|
| `SQL_ALLOW_INSERT` | Allow INSERT statements | `false` |
| `SQL_ALLOW_UPDATE` | Allow UPDATE statements | `false` |
| `SQL_ALLOW_DELETE` | Allow DELETE statements | `false` |
| `SQL_ALLOW_DDL` | Allow DDL statements (CREATE, ALTER, DROP, TRUNCATE) | `false` |

**MongoDB (`DB_TYPE=mongo`):**

| Variable | Description | Default |
|---|---|---|
| `MONGO_ALLOW_WRITE` | Allow insert/update operations | `false` |
| `MONGO_ALLOW_DELETE` | Allow delete operations | `false` |
| `MONGO_ALLOW_ADMIN` | Allow admin operations (reserved for future tools) | `false` |

**Redis (`DB_TYPE=redis`):**

| Variable | Description | Default |
|---|---|---|
| `REDIS_ALLOW_WRITE` | Allow write commands (SET, DEL, HSET, LPUSH, ...) | `false` |
| `REDIS_ALLOW_ADMIN` | Allow admin commands (FLUSHDB, CONFIG, EVAL, SCRIPT, ...) | `false` |

Unknown Redis commands are rejected regardless of permission flags -- the classifier ships an explicit allow-list, so new commands added by future Redis releases default to deny until the classifier is updated.

**Elasticsearch (`DB_TYPE=elasticsearch`):**

| Variable | Description | Default |
|---|---|---|
| `ES_ALLOW_WRITE` | Allow index/update operations | `false` |
| `ES_ALLOW_DELETE` | Allow delete operations | `false` |
| `ES_ALLOW_ADMIN` | Allow admin operations (reserved for future tools) | `false` |

### Examples

**Read-only access** (safest -- default):

```json
"env": {
  "DB_HOST": "127.0.0.1",
  "DB_USER": "readonly_user",
  "DB_PASSWORD": "secret",
  "DB_NAME": "production_db"
}
```

**Full read/write access** (development):

```json
"env": {
  "DB_HOST": "127.0.0.1",
  "DB_USER": "dev_user",
  "DB_PASSWORD": "secret",
  "DB_NAME": "dev_db",
  "SQL_ALLOW_INSERT": "true",
  "SQL_ALLOW_UPDATE": "true",
  "SQL_ALLOW_DELETE": "true",
  "SQL_ALLOW_DDL": "true"
}
```

**Insert-only** (data ingestion):

```json
"env": {
  "DB_HOST": "127.0.0.1",
  "DB_USER": "ingest_user",
  "DB_PASSWORD": "secret",
  "DB_NAME": "analytics_db",
  "SQL_ALLOW_INSERT": "true"
}
```

**PostgreSQL**:

```json
"env": {
  "DB_TYPE": "postgres",
  "DB_HOST": "127.0.0.1",
  "DB_USER": "postgres",
  "DB_PASSWORD": "secret",
  "DB_NAME": "my_app_db"
}
```

**MongoDB**:

```json
"env": {
  "DB_TYPE": "mongo",
  "DB_HOST": "127.0.0.1",
  "DB_USER": "appuser",
  "DB_PASSWORD": "secret",
  "DB_NAME": "my_app_db",
  "MONGO_ALLOW_WRITE": "false",
  "MONGO_ALLOW_DELETE": "false"
}
```

**Redis**:

```json
"env": {
  "DB_TYPE": "redis",
  "DB_HOST": "127.0.0.1",
  "DB_PASSWORD": "secret",
  "REDIS_DB": "0",
  "REDIS_ALLOW_WRITE": "false",
  "REDIS_ALLOW_ADMIN": "false"
}
```

**Elasticsearch** (basic auth):

```json
"env": {
  "DB_TYPE": "elasticsearch",
  "DB_HOST": "127.0.0.1",
  "DB_PORT": "9200",
  "DB_USER": "elastic",
  "DB_PASSWORD": "secret",
  "ES_ALLOW_WRITE": "false",
  "ES_ALLOW_DELETE": "false"
}
```

**Elasticsearch Cloud** (API key):

```json
"env": {
  "DB_TYPE": "elasticsearch",
  "DB_URI": "https://your-deployment.es.region.cloud:9200",
  "ES_API_KEY": "your-base64-api-key"
}
```

For replica sets or anything requiring a custom URI, set `DB_URI` instead:

```json
"env": {
  "DB_TYPE": "mongo",
  "DB_URI": "mongodb://user:secret@host1:27017,host2:27017/my_app_db?replicaSet=rs0",
  "DB_NAME": "my_app_db"
}
```

## MCP Tools

The tools registered at runtime depend on `DB_TYPE`. SQL deployments expose `sql_query`. MongoDB deployments expose the five `mongo_*` tools.

### sql_query

*(`DB_TYPE=mysql` or `postgres`)* Execute a SQL query against the connected SQL database.

**Parameters:**

| Name | Type | Required | Description |
|---|---|---|---|
| `sql` | string | yes | The SQL query to execute |

**Response format (SELECT):**

```json
{
  "rows": [
    { "id": 1, "name": "Alice", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": "bob@example.com" }
  ],
  "rowCount": 2,
  "executionTimeMs": 12,
  "truncated": false
}
```

When the query would return more than `MAX_ROWS` rows, `rows` is capped and `truncated: true` is set. Add `LIMIT/OFFSET` to your SQL to page through.

**Response format (INSERT/UPDATE/DELETE):**

```json
{
  "rows": [
    { "affectedRows": 3 }
  ],
  "rowCount": 3,
  "executionTimeMs": 45
}
```

**Permission denied response:**

```
Permission denied: INSERT operations are not allowed
Allowed operations: SELECT
```

### mongo_find

*(`DB_TYPE=mongo`)* Query documents in a collection.

| Name | Type | Required | Description |
|---|---|---|---|
| `collection` | string | yes | Collection name |
| `filter` | string | no | Query filter (MongoDB extended JSON) |
| `projection` | string | no | Projection JSON |
| `sort` | string | no | Sort JSON |
| `limit` | int | no | Max results (default 100, capped at `MAX_ROWS`) |
| `skip` | int | no | Skip first N documents for pagination (default 0) |

Response includes `truncated: true` if there are matching documents beyond what was returned. Page forward by increasing `skip` by the previous `rowCount`.

### mongo_aggregate

*(`DB_TYPE=mongo`)* Run an aggregation pipeline. Results are capped at `MAX_ROWS`; add `$skip`/`$limit` stages to page through.

| Name | Type | Required | Description |
|---|---|---|---|
| `collection` | string | yes | Collection name |
| `pipeline` | string | yes | JSON array of pipeline stages |

### mongo_insert

*(`DB_TYPE=mongo`, requires `MONGO_ALLOW_WRITE=true`)* Insert one or more documents.

| Name | Type | Required | Description |
|---|---|---|---|
| `collection` | string | yes | Collection name |
| `documents` | string | yes | Document JSON object or JSON array |

### mongo_update

*(`DB_TYPE=mongo`, requires `MONGO_ALLOW_WRITE=true`)* Update documents.

| Name | Type | Required | Description |
|---|---|---|---|
| `collection` | string | yes | Collection name |
| `filter` | string | yes | Filter JSON |
| `update` | string | yes | Update JSON (e.g. `{"$set": {...}}`) |
| `multi` | bool | no | Update all matching docs (default `false` = `updateOne`) |

### mongo_delete

*(`DB_TYPE=mongo`, requires `MONGO_ALLOW_DELETE=true`)* Delete documents.

| Name | Type | Required | Description |
|---|---|---|---|
| `collection` | string | yes | Collection name |
| `filter` | string | yes | Filter JSON |
| `multi` | bool | no | Delete all matching docs (default `false` = `deleteOne`) |

### redis_command

*(`DB_TYPE=redis`)* Execute an arbitrary Redis command. The command name is classified as READ, WRITE, or ADMIN; unknown commands are rejected.

| Name | Type | Required | Description |
|---|---|---|---|
| `command` | string | yes | Redis command name (e.g. `GET`, `SET`, `LRANGE`, `INFO`) |
| `args` | string | no | Arguments as a JSON array of strings, e.g. `["mykey", "myvalue"]` |

**Response:**

```json
{
  "result": "myvalue",
  "executionTimeMs": 1
}
```

For list-returning commands (LRANGE, HGETALL, etc.) the `result` is a JSON array. Bulk-string replies are decoded as UTF-8.

### es_search

*(`DB_TYPE=elasticsearch`)* Run a search against an index using the Query DSL.

| Name | Type | Required | Description |
|---|---|---|---|
| `index` | string | yes | Index name or alias |
| `query` | string | no | Search body (Query DSL JSON, e.g. `{"query":{"match":{"title":"foo"}}}`) |
| `size` | int | no | Max results (default 10, capped at `MAX_ROWS`) |
| `from` | int | no | From offset (default 0) |

Returns the raw Elasticsearch response (`hits.hits`, `aggregations`, etc.) with `executionTimeMs` and `truncated` appended. `truncated: true` means `hits.total.value` exceeds `from + size`; increase `from` to page.

### es_get

*(`DB_TYPE=elasticsearch`)* Fetch a document by ID.

| Name | Type | Required | Description |
|---|---|---|---|
| `index` | string | yes | Index name |
| `id` | string | yes | Document ID |

### es_index

*(`DB_TYPE=elasticsearch`, requires `ES_ALLOW_WRITE=true`)* Index a document (create or replace).

| Name | Type | Required | Description |
|---|---|---|---|
| `index` | string | yes | Index name |
| `document` | string | yes | Document JSON |
| `id` | string | no | Document ID. If omitted, Elasticsearch auto-generates one. |

### es_update

*(`DB_TYPE=elasticsearch`, requires `ES_ALLOW_WRITE=true`)* Partial update on an existing document.

| Name | Type | Required | Description |
|---|---|---|---|
| `index` | string | yes | Index name |
| `id` | string | yes | Document ID |
| `doc` | string | yes | Partial fields JSON to merge (wrapped as `{"doc": ...}` server-side) |

### es_delete

*(`DB_TYPE=elasticsearch`, requires `ES_ALLOW_DELETE=true`)* Delete a document by ID.

| Name | Type | Required | Description |
|---|---|---|---|
| `index` | string | yes | Index name |
| `id` | string | yes | Document ID |

## MCP Resources

### db://tables

*(`DB_TYPE=mysql` or `postgres`)* Lists all tables in the connected database with metadata.

**Response:**

```json
[
  {
    "tableName": "users",
    "estimatedRowCount": 15228,
    "dataSizeBytes": 2637824,
    "indexSizeBytes": 2129920,
    "createTime": "2026-03-31T15:00:47",
    "updateTime": null
  }
]
```

### db://tables/{tableName}

*(`DB_TYPE=mysql` or `postgres`)* Returns column details for a specific table.

**Example:** `db://tables/users`

**Response:**

```json
[
  {
    "columnName": "id",
    "dataType": "bigint",
    "columnType": "bigint unsigned",
    "nullable": "NO",
    "columnKey": "PRI",
    "defaultValue": null,
    "extra": "auto_increment"
  },
  {
    "columnName": "name",
    "dataType": "varchar",
    "columnType": "varchar(255)",
    "nullable": "YES",
    "columnKey": "",
    "defaultValue": null,
    "extra": ""
  }
]
```

### db://collections

*(`DB_TYPE=mongo`)* Lists all collections in the connected database with estimated document count.

```json
[
  { "collectionName": "users", "estimatedDocumentCount": 15228 },
  { "collectionName": "orders", "estimatedDocumentCount": 42130 }
]
```

### db://collections/{collectionName}

*(`DB_TYPE=mongo`)* Returns a sample document plus the indexes for the collection.

```json
{
  "collection": "users",
  "sampleDocument": { "_id": {"$oid": "..."}, "name": "Alice", "email": "alice@example.com" },
  "indexes": [
    { "v": 2, "key": {"_id": 1}, "name": "_id_" },
    { "v": 2, "key": {"email": 1}, "name": "email_1", "unique": true }
  ]
}
```

### db://keys

*(`DB_TYPE=redis`)* Sample of up to 100 keys via SCAN. For pattern search, call `redis_command(SCAN, ["0","MATCH","prefix:*","COUNT","100"])`.

```json
[
  { "key": "user:1", "type": "hash", "ttlSeconds": -1 },
  { "key": "session:abc", "type": "string", "ttlSeconds": 3600 }
]
```

### db://keys/{key}

*(`DB_TYPE=redis`)* Returns the type, TTL, and decoded value of a specific key. The value shape depends on the type (string, list, hash, set, zset members+scores).

```json
{
  "key": "user:1",
  "type": "hash",
  "ttlSeconds": -1,
  "value": { "name": "Alice", "email": "alice@example.com" }
}
```

### db://indices

*(`DB_TYPE=elasticsearch`)* Lists all indices (via `_cat/indices?format=json&bytes=b`) with health, docs count, and store size.

```json
[
  { "health": "green", "status": "open", "index": "users", "docs.count": "1523", "store.size": "204800" }
]
```

### db://indices/{indexName}

*(`DB_TYPE=elasticsearch`)* Returns settings and mappings for a specific index (raw `GET /<index>` response).

## Testing with MCP Inspector

[MCP Inspector](https://github.com/modelcontextprotocol/inspector) (v0.21.1+) is a visual testing and debugging tool for MCP servers. It provides a web UI to connect to your server, browse its capabilities, and invoke tools/resources interactively.

### Install and run

```bash
# Run directly (no install needed, downloads to npx cache)
npx @modelcontextprotocol/inspector@0.21.1

# Or install globally
npm install -g @modelcontextprotocol/inspector@0.21.1
mcp-inspector
```

This starts two services:
- **Inspector UI** at `http://localhost:6274` -- the web interface you interact with
- **MCP Proxy** at `http://localhost:6277` -- bridges the UI to your STDIO server

### Connect to java-db-mcp

In the Inspector UI at `http://localhost:6274`:

1. Set **Transport** to `STDIO`
2. Set **Command** to `java`
3. Set **Arguments** to `-jar /absolute/path/to/java-db-mcp.jar`
4. Add your environment variables under **Environment Variables**:

   | Key | Value |
   |---|---|
   | `DB_TYPE` | `mysql` |
   | `DB_HOST` | `127.0.0.1` |
   | `DB_PORT` | `3306` |
   | `DB_USER` | `root` |
   | `DB_PASSWORD` | *your password* |
   | `DB_NAME` | *your database* |

5. Click **Connect**

### Inspector tabs

Once connected, the Inspector provides three tabs matching MCP capabilities:

| Tab | What it shows | Try this |
|---|---|---|
| **Tools** | Lists all tools with their JSON schema, descriptions, and parameter definitions | Select `sql_query`, enter `SELECT * FROM your_table LIMIT 5` in the `sql` field, click Run |
| **Resources** | Lists all resources with their URIs | Click `db://tables` to see all tables, or enter a table name in `db://tables/{tableName}` |
| **Prompts** | Lists prompt templates (none currently exposed) | -- |

### Custom ports

If the default ports conflict with other services:

```bash
# Custom UI port (CLIENT_PORT) and proxy port (SERVER_PORT)
CLIENT_PORT=8080 SERVER_PORT=8081 npx @modelcontextprotocol/inspector@0.21.1
```

## Architecture

```
com.wmatech.java_db_mcp
├── JavaDBMcpApplication.java                 # Spring Boot entry point
├── config/
│   └── AutoconfigExclusions.java             # excludes JDBC autoconfig when DB_TYPE is non-SQL
├── common/
│   ├── JsonFormatter.java                    # shared JSON pretty-printing
│   └── PermissionDeniedException.java        # shared permission-deny signal
├── sql/                                      # active when DB_TYPE in (mysql, postgres)
│   ├── SqlParser.java                        # JSqlParser-based query type detection
│   ├── QueryType.java
│   ├── PermissionService.java
│   ├── QueryService.java                     # SQL execution with read/write transactions
│   ├── SqlPermissionProperties.java
│   ├── tool/SqlQueryTool.java                # @McpTool sql_query
│   ├── resource/SqlTableResource.java        # @McpResource db://tables, db://tables/{name}
│   └── metadata/
│       ├── MetadataProvider.java             # interface
│       ├── MySqlMetadataProvider.java        # MySQL information_schema queries
│       └── PostgresMetadataProvider.java     # Postgres pg_class + information_schema queries
├── mongo/                                    # active when DB_TYPE=mongo
│   ├── MongoClientConfig.java                # builds MongoClient + MongoDatabase
│   ├── MongoPermissionProperties.java
│   ├── MongoPermissionService.java
│   ├── tool/                                 # five @McpTool implementations
│   │   ├── MongoFindTool.java                # mongo_find
│   │   ├── MongoAggregateTool.java           # mongo_aggregate
│   │   ├── MongoInsertTool.java              # mongo_insert
│   │   ├── MongoUpdateTool.java              # mongo_update
│   │   └── MongoDeleteTool.java              # mongo_delete
│   └── resource/MongoCollectionResource.java # @McpResource db://collections, db://collections/{name}
├── redis/                                    # active when DB_TYPE=redis
│   ├── RedisClientConfig.java                # builds JedisPooled
│   ├── RedisVerb.java                        # enum: READ, WRITE, ADMIN
│   ├── RedisPermissionProperties.java
│   ├── RedisPermissionService.java
│   ├── RedisCommandClassifier.java           # command -> RedisVerb allow-list
│   ├── tool/RedisCommandTool.java            # @McpTool redis_command
│   └── resource/RedisKeyResource.java        # @McpResource db://keys, db://keys/{key}
└── es/                                       # active when DB_TYPE=elasticsearch
    ├── EsRestClient.java                     # JDK HttpClient wrapper, basic/API-key auth
    ├── EsPermissionProperties.java
    ├── EsPermissionService.java
    ├── tool/                                 # five @McpTool implementations
    │   ├── EsSearchTool.java                 # es_search
    │   ├── EsGetTool.java                    # es_get
    │   ├── EsIndexTool.java                  # es_index
    │   ├── EsUpdateTool.java                 # es_update
    │   └── EsDeleteTool.java                 # es_delete
    └── resource/EsIndexResource.java         # @McpResource db://indices, db://indices/{name}
```

### How it works

`DB_TYPE` is read on startup. Spring profile activation + `@ConditionalOnExpression` / `@ConditionalOnProperty` ensure only one DB family's beans load per process. An `EnvironmentPostProcessor` (`AutoconfigExclusions`) strips JDBC autoconfiguration when `DB_TYPE` is not SQL so the app boots without a `spring.datasource.url`.

**SQL flow** (`mysql` / `postgres`):

1. AI client calls `sql_query` with a SQL string.
2. **SqlParser** classifies the statement via JSqlParser.
3. **PermissionService** checks the type against `SQL_ALLOW_*` env vars.
4. **QueryService** executes -- `SELECT` in a read-only transaction, writes in a regular transaction with commit/rollback.
5. Results return as JSON.

Resources delegate to a `MetadataProvider` (`MySqlMetadataProvider` or `PostgresMetadataProvider`) which queries each DB's catalog and shapes results into a uniform JSON response.

**MongoDB flow** (`mongo`):

1. AI client calls one of the five `mongo_*` tools.
2. Write/delete tools check **MongoPermissionService** against `MONGO_ALLOW_*` env vars.
3. Documents flow through `org.bson.Document.parse` (input) and `Document.toJson` (output) so MongoDB extended JSON (`{"$oid": ...}`, `{"$date": ...}`) round-trips correctly.

Resources use `mongoDatabase.listCollectionNames()`, `estimatedDocumentCount()`, and `listIndexes()` to produce schema-discovery output.

**Redis flow** (`redis`):

1. AI client calls `redis_command(command, args)`.
2. **RedisCommandClassifier** looks up the command name in its allow-list; unknown commands raise `PermissionDeniedException`.
3. **RedisPermissionService** checks the verb tier against `REDIS_ALLOW_*` env vars.
4. **JedisPooled** dispatches the command via `sendCommand`; the response is normalized (`byte[]` -> UTF-8 string, nested lists recursed).

Resources use `SCAN` + type-specific decoders (`GET`, `HGETALL`, `LRANGE`, `SMEMBERS`, `ZRANGE WITHSCORES`).

**Elasticsearch flow** (`elasticsearch`):

1. AI client calls one of `es_search`, `es_get`, `es_index`, `es_update`, `es_delete`.
2. Write/update/delete tools call **EsPermissionService** which checks `ES_ALLOW_*` env vars.
3. **EsRestClient** (JDK `HttpClient`) sends the JSON body to the corresponding REST endpoint (`/<index>/_search`, `/<index>/_doc/<id>`, `/<index>/_update/<id>`).
4. The full Elasticsearch JSON response is returned to the AI client with `executionTimeMs` appended.

Resources call `_cat/indices?format=json&bytes=b` and `GET /<index>` to expose schema discovery.

## Security Considerations

- **Read-only by default**: All write operations must be explicitly enabled
- **SQL parsing**: Queries are parsed by JSqlParser before execution to accurately detect the operation type -- comments, CTEs, and other tricks cannot bypass permission checks
- **Transaction safety**: SELECT runs in read-only transactions; write operations use transactions with automatic rollback on failure
- **No raw credential storage**: Database credentials are passed as environment variables, never stored in config files
- **Connection pooling**: HikariCP manages connections with a configurable pool size (default: 10)

**Recommendations:**

- Use a dedicated database user with minimal privileges for each project
- Keep write permissions disabled in production environments
- Never expose the MCP server over HTTP without authentication (STDIO is local-only by design)

## Tech Stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.6 |
| Spring AI MCP | 2.0.0-M6 |
| JSqlParser | 5.3 |
| MySQL Connector/J | *(managed by Spring Boot)* |
| PostgreSQL JDBC | *(managed by Spring Boot)* |
| MongoDB Driver (sync) | *(managed by Spring Boot)* |
| Jedis (Redis client) | *(managed by Spring Boot)* |
| Elasticsearch | via JDK `java.net.http.HttpClient` (no extra dep) |
| HikariCP | *(managed by Spring Boot)* |
| JUnit | 6 |
| Testcontainers | 1.21.4 |

## Development

### Build

```bash
./mvnw clean package
```

### Run tests

```bash
./mvnw test
```

### Run locally

```bash
DB_HOST=localhost DB_NAME=mydb DB_USER=root DB_PASSWORD=secret \
  java -jar java-db-mcp.jar
```

## Roadmap

- [x] MySQL support
- [x] PostgreSQL support
- [x] MongoDB support
- [x] Redis support
- [x] Elasticsearch / OpenSearch support
- [x] Query result pagination (`MAX_ROWS` cap + `truncated` flag)
- [ ] HTTP/SSE transport for remote deployment
- [ ] Schema-specific permission overrides

## License

MIT
