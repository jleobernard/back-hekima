update note set files = ('{"files":[{"mime_type":"'|| mime_type || '", "file_id": "' || file_id || '"}]}')::json where file_id is not null;