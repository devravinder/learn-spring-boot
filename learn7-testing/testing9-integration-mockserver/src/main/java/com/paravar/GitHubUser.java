package com.paravar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubUser {
	private Long id;
	private String login;
	private String name;
	private String company;
	private String blog;
	private String location;
	private String email;
	private String bio;
	@JsonProperty("twitter_username")
	private String twitterUsername;
	// Jackson 3 fails on missing primitives by default; wrappers allow absent fields
	@JsonProperty("public_repos")
	private Integer publicRepos;
	private Integer followers;
	private Integer following;
	private Boolean hireable;
}
