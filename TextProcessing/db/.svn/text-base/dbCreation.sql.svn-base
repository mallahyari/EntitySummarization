DROP TABLE IF EXISTS invertedindex;


-------------------
-- Entity: invertedindex
-------------------
CREATE TABLE invertedindex (
  sid                  serial PRIMARY KEY,
  word       	       VARCHAR(256) NOT NULL,
  category_id          integer NOT NULL,
  frequency            smallint NOT NULL
  
);  

CREATE INDEX word_index on invertedindex (word);
CREATE INDEX category_id_index on invertedindex (category_id);



-------------------
-- Entity: category_entity
-------------------
CREATE TABLE category_entity (
  sid                  serial PRIMARY KEY,
  category_id          integer NOT NULL,
  entity_id            integer NOT NULL
  
);  

CREATE INDEX category_entity_category_id_index on category_entity (category_id);
CREATE INDEX category_entity_entity_id_index on category_entity (entity_id);




-------------------
-- Entity: entity_incominglink
-------------------
CREATE TABLE entity_incominglink (
  sid                  serial PRIMARY KEY,
  incoming_entity_id   integer NOT NULL,
  entity_id            integer NOT NULL
  
);  

CREATE INDEX entity_incominglink_incoming_entity_id_index on entity_incominglink (incoming_entity_id);
CREATE INDEX entity_incominglink_entity_id_index on entity_incominglink (entity_id);












