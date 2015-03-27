-- name: read-apis
SELECT application_id, status
  FROM api;

--name: read-api
SELECT application_d, status, type, name, version, url, ui, definition
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
