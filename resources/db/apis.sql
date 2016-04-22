-- name: read-apis
SELECT application_id, status, name, version
  FROM api;

-- name: search-apis
SELECT application_id,
       status,
       name,
       version,
       ts_rank_cd(vector, query) AS matched_rank,
       ts_headline('simple', definition, query) AS matched_definition
FROM (SELECT application_id,
             status,
             name,
             version,
             definition,
             setweight(to_tsvector('simple', name), 'A')
             || setweight(to_tsvector('simple', COALESCE(version, '')), 'B')
             || setweight(to_tsvector('simple', COALESCE(definition, '')), 'C')
               as vector
      FROM api) as apis,
  to_tsquery('simple', :searchquery) query
WHERE query @@ vector
ORDER BY matched_rank DESC;

--name: read-api
SELECT application_id, status, type, name, version, url, ui, definition
  FROM api
 WHERE application_id = :application_id;

 --name: read-api-definition
 SELECT definition
   FROM api
  WHERE application_id = :application_id;

-- name: create-or-update-api!
WITH api_update AS (
     UPDATE api
        SET status         = :status,
            type           = :type,
            name           = :name,
            version        = :version,
            url            = :url,
            ui             = :ui,
            definition     = :definition
      WHERE application_id = :application_id
  RETURNING *)
INSERT INTO api
            (application_id, status, type, name, version, url, ui, definition)
     SELECT :application_id, :status, :type, :name, :version, :url, :ui, :definition
      WHERE NOT EXISTS (SELECT * FROM api_update);
