package pl.home.couchdbpoc.data;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SyncStatusDocument {

	@SerializedName("_id")
	private String id = "sql_to_couchdb_sync_marker";

	@SerializedName("_rev")
	private String rev;

	private LocalDateTime createdAt = LocalDateTime.now();

}
