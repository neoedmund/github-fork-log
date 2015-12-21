package neoe.github;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import neoe.util.Base64;
import neoe.util.Config;
import neoe.util.FileUtil;
import neoe.util.Log;
import neoe.util.PyData;

public class LogForks {
	static boolean USE_AUTHOR_FILTER = true;

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println(
					"param: <repo_name(format: owner/repo, repo_name must be original name not forked)>  <max-fork-scan>");
			return;
		}
		Log.stdout = true;
		String repo = args[0];
		int maxsize = Integer.parseInt(args[1]);
		new LogForks().run(repo, maxsize);
		Log.log("Program end.");

	}

	private String user;
	private String basicAuth;

	public void run(String repo, int maxsize) throws Exception {
		login();
		showRateLimit();
		List forks = fetchAllForks(repo, maxsize);// cannot too many
		int total = forks.size();
		int index = 0;
		List commits = new ArrayList();
		for (Object o : forks) {
			Map m = (Map) o;
			String name = (String) m.get("full_name");
			List com = fetchCommitsOfRepo(name, 10);
			if (!USE_AUTHOR_FILTER) {
				for (Object o2 : com) {
					Map m2 = (Map) o2;
					if (Config.get(m2, "commit.author.name").equals(Config.get(m2, "commit.committer.name"))) {
						// avoid cross ref, use only original
						commits.add(m2);
					}
				}
			}
			if (USE_AUTHOR_FILTER) {
				// already filterd in fetchCommitsOfRepo(? Not now)
				commits.addAll(com);
			}
			index++;
			Log.log(String.format("[D](%d/%d)%s, add=%d, sum=%d", index, total, name, com.size(), commits.size()));
		}
		sortCommitsByTimeDesc(commits);
		index = 0;

		for (Object o : commits) {
			index++;
			Log.log("[R]" + index + ":" + reformCommit((Map) o, index < 50));
		}

		showRateLimit();
	}

	private void showRateLimit() {
		Log.log("your RateLimit:" + githubHttpsGet("https://api.github.com/rate_limit"));
	}

	private void login() {
		String userCredentials = LoginDialog.login();
		int p1 = userCredentials.indexOf(':');
		user = userCredentials.substring(0, p1);
		basicAuth = "Basic " + Base64.encodeBytes(userCredentials.getBytes());
	}

	private String reformCommit(Map com, boolean fetchStat) {
		String url = (String) com.get("url");

		String stat = "NA";
		if (fetchStat) { // get stat
			String detail = githubHttpsGet(url);
			try {
				if (detail != null) {
					Map m = (Map) PyData.parseAll(detail);
					stat = String.format("+%s -%s", Config.get(m, "stats.additions"), Config.get(m, "stats.deletions"));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return String.format("repo:%s,author:%s,date:%s,stat:%s, msg:%s", Config.get(com, "_repo_"),
				Config.get(com, "commit.author.name"), Config.get(com, "commit.author.date"), stat,
				Config.get(com, "commit.message"));
	}

	private List fetchCommitsOfRepo(String name, int maxsize) throws Exception {
		// Log.log("fetchCommitsOfRepo " + name);
		int p1 = name.indexOf('/');
		String login = name.substring(0, p1);
		String url;
		if (USE_AUTHOR_FILTER) {
			url = String.format("/repos/%s/commits?author=%s", name, URLEncoder.encode(login, "UTF8"));
		} else {
			url = String.format("/repos/%s/commits", name);
		}

		List allItems = githubGetList(url, maxsize);
		sortCommitsByTimeDesc(allItems);
		if (maxsize > 0 && allItems.size() > maxsize) {
			allItems = allItems.subList(0, maxsize);
		}
		for (Object o : allItems) {
			Map m = (Map) o;
			m.put("_repo_", name);
		}
		return allItems;
	}

	private void sortCommitsByTimeDesc(List commits) {
		Collections.sort(commits, new Comparator() {

			@Override
			public int compare(Object o1, Object o2) {
				Map m1 = (Map) o1;
				Map m2 = (Map) o2;
				String s1 = (String) Config.get(m1, "commit.committer.date");
				String s2 = (String) Config.get(m2, "commit.committer.date");
				return s2.compareTo(s1);
			}
		});
	}

	private List fetchAllForks(String repo, int maxsize) throws Exception {
		Log.log("fetchAllForks of " + repo);
		String url = String.format("/repos/%s/forks", repo);
		List allItems = githubGetList(url, maxsize);
		return allItems;
	}

	private List githubGetList(String cmd, int maxsize) throws Exception {
		int page = 1;
		int pagesize = 100;
		if (maxsize > 0 && maxsize < pagesize)
			pagesize = maxsize;
		List all = new ArrayList();
		while (true) {
			// Log.log(String.format("fetch:page %d, size %d", page, pagesize));
			String addon = "?";
			if (cmd.indexOf('?') >= 0)
				addon = "&";
			String url = String.format("https://api.github.com%s%spage=%d&per_page=%d", cmd, addon, page, pagesize);
			String json = githubHttpsGet(url);
			if (json == null)
				return null;
			try {
				List list = (List) PyData.parseAll(json);
				all.addAll(list);
				Log.log("[D]get size: " + list.size() + " , sum=" + all.size());
				if (maxsize > 0 && all.size() >= maxsize)
					break;
				if (list.size() < pagesize)
					break;
				page++;
			} catch (Exception ex) {
				ex.printStackTrace();
				Log.log("unexpected response:" + json);
			}
		}
		return all;
	}

	private String githubHttpsGet(String url) {
		int tries = 10;
		while (true) {
			HttpsURLConnection con = null;
			try {
				Log.log("access:" + url);
				URL myurl = new URL(url);
				con = (HttpsURLConnection) myurl.openConnection();
				con.setReadTimeout(30000);
				con.setRequestProperty("Authorization", basicAuth);
				con.setRequestProperty("User-Agent", user);
				con.setDoInput(true);
				con.setDoOutput(true);
				InputStream ins = con.getInputStream();
				return FileUtil.readString(ins, null);
			} catch (Exception ex) {
				ex.printStackTrace();
				if (con != null) {
					try {
						InputStream ins = con.getErrorStream();
						if (ins != null) {
							String s = FileUtil.readString(ins, null);
							Log.log("[E]" + s);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (tries > 0) {
					Log.log("retry " + tries);
					tries--;
				} else {
					return null;
				}
			}
		}
	}

}
