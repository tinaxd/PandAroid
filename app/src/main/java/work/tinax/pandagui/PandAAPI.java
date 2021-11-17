// Copyright 2020 tinaxd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package work.tinax.pandagui;

import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

public class PandAAPI implements AutoCloseable {
	
	private LogTarget log = null;
	private boolean loginTried = false;

	private CloseableHttpClient client;

	private static final Pattern TAG_REMOVE_PATTERN = Pattern.compile("<.+?>");

	/** PandAAPI オブジェクトを作成する.
	 *  @param log ログの出力先. ログを出力しない場合は null を設定する.
	 */
	public PandAAPI(LogTarget log) {
		this.log = log;
		
		// HTTP client を作成
		client = HttpClients.createDefault();
	}

	/**
	 * PandAAPI オブジェクトを作成する.
	 * ログは出力しない.
	 */
	public PandAAPI() {
		this(null);
	}
	
	/**
	 * ログを出力する.
	 * @param line 出力する文字列.
	 */
	private void logLine(String line) {
		// log が null の場合はログを行わない
		if (log != null) {
			log.logLine(line);
		}
	}

	/** ログイントークンを取得する.
	 * @returns ログイントークンを表す文字列.
	 * @throws PandAAPIException 取得に失敗した場合.
     */
	public String getLoginToken() throws PandAAPIException {
		HttpGet get = new HttpGet("https://panda.ecs.kyoto-u.ac.jp/cas/login?service=https%3A%2F%2Fpanda.ecs.kyoto-u.ac.jp%2Fsakai-login-tool%2Fcontainer");
		try (CloseableHttpResponse response = client.execute(get)) {
			if (response.getCode() != 200) {
				throw new PandAAPIException("/login not 200");
			}
			HttpEntity entity = response.getEntity();
			String text = EntityUtils.toString(entity);
			Pattern pat = Pattern.compile("<input type=\"hidden\" name=\"lt\" value=\"(.+)\" \\/>");
			Matcher match = pat.matcher(text);
			
			if (match.find()) {
				return match.group(1);
			}
			throw new PandAAPIException("cannot get login token");
		} catch (IOException | ParseException | PandAAPIException ex) {
			throw new PandAAPIException("IO error", ex);
		}
	}

	/**
	 * ログインする.
	 * @param ecsId ECS-ID
	 * @param password パスワード
	 * @throws PandAAPIException ログインに失敗した場合. ユーザー名やパスワードが誤っているためログインに失敗する場合は, この例外は投げられない.
	 * @see PandAAPI#isLoggedIn()
	 */
	public void login(String ecsId, String password) throws PandAAPIException {
		String lt = getLoginToken();
		HttpPost post = new HttpPost("https://panda.ecs.kyoto-u.ac.jp/cas/login?service=https%3A%2F%2Fpanda.ecs.kyoto-u.ac.jp%2Fsakai-login-tool%2Fcontainer");
		// POST する内容を作成
		List<NameValuePair> form = new ArrayList();
		form.add(new BasicNameValuePair("_eventId", "submit"));
		form.add(new BasicNameValuePair("execution", "e1s1"));
		form.add(new BasicNameValuePair("lt", lt));
		form.add(new BasicNameValuePair("password", password));
		form.add(new BasicNameValuePair("username", ecsId));
		post.setEntity(new UrlEncodedFormEntity(form));
		// ログインを試行したことを記録
		loginTried = true;
		try (CloseableHttpResponse response = client.execute(post)) {
			EntityUtils.consume(response.getEntity());
			logLine("ログイン試行");
		} catch (IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}

	/**
	 * ログインしているか確認する.
	 * @return ログインしていれば true, そうでなければ false.
	 */
	public boolean isLoggedIn() throws PandAAPIException {
		HttpGet req = new HttpGet("https://panda.ecs.kyoto-u.ac.jp/portal/");
		try (CloseableHttpResponse response = client.execute(req)) {
			String text = EntityUtils.toString(response.getEntity());
			logLine("ログイン確認");
			// "loggedIn": true という文字列が含まれていれば, ログインしていることになる
			return Pattern.compile("\"loggedIn\": true").matcher(text).find();
		} catch (ParseException | IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}

	/**
	 * ログアウトする.
	 * @throws PandAAPIException ログアウトできなかった場合.
	 * すでにログアウトしていた場合は例外は投げない.
	 */
	public void logout() throws PandAAPIException {
		HttpGet req = new HttpGet("https://panda.ecs.kyoto-u.ac.jp/portal/logout");
		try (CloseableHttpResponse response = client.execute(req)) {
			EntityUtils.consume(response.getEntity());
			logLine("ログアウト");
		} catch (IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}

	/**
	 * "2020/11/07 20:17" のような形式の文字列から LocalDateTime を生成する.
	 * @param due 変換する文字列
	 * @return 生成した LocalDateTime
	 * @throws MalformedKadaiJsonException 文字列の形式が無効な場合
	 */
	private static LocalDateTime makeDateTime(String due) {
		// "2020/11/07 20:30" のような形式
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("y/M/d H:m");
		try {
			return formatter.parse(due, new TemporalQuery<LocalDateTime>() {
				@Override
				public LocalDateTime queryFrom(TemporalAccessor temporalAccessor) {
					return LocalDateTime.from(temporalAccessor);
				}
			});
		} catch (DateTimeParseException ex) {
			throw new MalformedKadaiJsonException("invalid time format: \"" + due + "\"", ex);
		}
	}

	/**
	 * 文字列からHTMLタグを取り除く.
	 * @param text 文字列
	 * @return HTMLタグが取り除かれた文字列
	 */
	public static String removeHttpTags(String text) {
		return TAG_REMOVE_PATTERN.matcher(text).replaceAll("");
	}

	/**
	 * 課題のリストを含む JSON から Kadai のリストを作成する.
	 * @param root 課題のリストを含む JSON
	 * @param lecture 講義名
	 * @return Kadai のリスト
	 * @throws MalformedKadaiJsonException JSON の形式が無効な場合
	 */
	private static List<Kadai> makeKadaiListFromJson(JsonNode root, String lecture) throws NoKadaiInfoException {
		Log.d("makeKadaiListFromJson lecture", lecture);
		JsonNode kadais = root.get("assignment_collection");
		if (kadais == null) {
			throw new NoKadaiInfoException("assignment_collection missing");
		}
		List<Kadai> result = new ArrayList<>();
		int index = 0;
		while (kadais.has(index)) {
			JsonNode kadai = kadais.get(index);
			String entityId = kadai.get("entityId").textValue();
			String title = kadai.get("title").textValue();
			LocalDateTime due = LocalDateTime.ofEpochSecond(kadai.get("dueTime").get("epochSecond").longValue(), 0, ZoneOffset.ofHours(9));
			String description = kadai.get("instructions").textValue();
			result.add(new KadaiBuilder()
							.id(entityId)
							.title(title)
							.due(due)
							.description(description)
							.lecture(lecture)
							.build());
			index++;
		}
		return result;
	}

	private static List<Kadai> makeQuizListFromJson(JsonNode root, String lecture) throws NoKadaiInfoException {
		Log.d("makeQuizListFromJson lecture", lecture);
		JsonNode quizRoot = root.get("sam_pub_collection");
		if (quizRoot == null) {
			throw new NoKadaiInfoException("sam_pub_collection missing");
		}
		List<Kadai> result = new ArrayList<>();
		for (int index=0; quizRoot.has(index); index++) {
			JsonNode quiz = quizRoot.get(index);
			String entityId = Kadai.QUIZ_ID_PREFIX + quiz.get("publishedAssessmentId").intValue();
			String title = quiz.get("title").textValue();
			LocalDateTime due = LocalDateTime.ofEpochSecond(quiz.get("dueDate").longValue() / 1000L, 0, ZoneOffset.ofHours(9));
			result.add(new KadaiBuilder()
					.id(entityId)
					.title(title)
					.due(due)
					.description("")
					.lecture(lecture)
					.asQuiz()
					.build());
		}
		return result;
	}
	
	/**
	 * JSON 文字列から課題のリストを作成する
	 * @param t JSON 文字列
	 * @param lecture 講義名
	 * @return Kadai のリスト
	 * @throws PandAAPIException t が有効な JSON でない場合
	 * @throws MalformedKadaiJsonException t が JSON としてパースできるが, makeKadaiListFromJson が処理できる形式ではない場合
	 */
	private static List<Kadai> makeKadaiListFromJsonString(String t, String lecture) throws PandAAPIException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return makeKadaiListFromJson(mapper.readTree(t), lecture);
		} catch (JsonProcessingException e) {
			throw new PandAAPIException("could not parse json", e);
		}
	}

	private static List<Kadai> makeQuizListFromJsonString(String t, String lecture) throws PandAAPIException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return makeQuizListFromJson(mapper.readTree(t), lecture);
		} catch (JsonProcessingException e) {
			throw new PandAAPIException("could not parse json", e);
		}
	}

	/**
	 * サイトID siteId 内の全課題を取得する 
	 * @param siteId サイトID
	 * @return Kadai のリスト
	 * @throws PandAAPIException 通信エラーが発生した場合
	 */
	public List<Kadai> getAssignments(String siteId) throws PandAAPIException {
		updateSiteIdCache();
		HttpGet req =
				new HttpGet(String.format("https://panda.ecs.kyoto-u.ac.jp/direct/assignment/site/%s.json", siteId));
		try (CloseableHttpResponse response = client.execute(req)) {
			String lectureName = siteIdCache.getOrDefault(siteId, siteId);
			try {
				return makeKadaiListFromJsonString(
						EntityUtils.toString(response.getEntity()), lectureName);
			} catch (NoKadaiInfoException e) {
				return new ArrayList<>();
			}
		} catch (ParseException | IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}

	public List<Kadai> getQuiz(String siteId) throws PandAAPIException {
		updateSiteIdCache();
		HttpGet req =
				new HttpGet(String.format("https://panda.ecs.kyoto-u.ac.jp/direct/sam_pub/context/%s.json", siteId));
		try (CloseableHttpResponse response = client.execute(req)) {
			String lectureName = siteIdCache.getOrDefault(siteId, siteId);
			try {
				return makeQuizListFromJsonString(
						EntityUtils.toString(response.getEntity()), lectureName);
			} catch (NoKadaiInfoException e) {
				return new ArrayList<>();
			}
		} catch (ParseException | IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}
	
	// map SiteId -> LectureName
	private Map<String, String> siteIdCache = new HashMap<>();
	private boolean siteIdCacheUpdated = false;

	/**
	 * ユーザーがアクティブサイトとして登録しているサイトのIDを取得する.
	 * @return サイトIDのリスト
	 * @throws PandAAPIException 通信エラーが発生した場合
	 */
	public List<SiteIdInfo> fetchSiteIds() throws PandAAPIException {
		HttpGet req = new HttpGet("https://panda.ecs.kyoto-u.ac.jp/portal/");
		try (CloseableHttpResponse response = client.execute(req)) {
			String portal = EntityUtils.toString(response.getEntity());
			Pattern re = Pattern.compile(
					"<a href=\"https://panda\\.ecs\\.kyoto-u\\.ac\\.jp/portal/site-reset/(.+?)\" title=\"(.+?)\"");	
			Matcher matcher = re.matcher(portal);
			ArrayList<SiteIdInfo> result = new ArrayList();
			while (matcher.find()) {
				result.add(
						new SiteIdInfo(
								StringEscapeUtils.unescapeHtml4(matcher.group(1)),
								StringEscapeUtils.unescapeHtml4(matcher.group(2))));
			}
			siteIdCache.clear();
			for (SiteIdInfo info : result) {
				siteIdCache.put(info.getSiteId(), info.getLectureName());
			}
			siteIdCacheUpdated = true;
			return result;
		} catch (ParseException | IOException e) {
			throw new PandAAPIException("IO error", e);
		}
	}
	
	private void updateSiteIdCache() throws PandAAPIException {
		if (!siteIdCacheUpdated) {
			fetchSiteIds();
			siteIdCacheUpdated = true;
		}
	}

	/**
	 * PandAAPI オブジェクトを作成し, ログインする.
	 * 次のように利用することが想定されている.
	 * <pre>
	 * {@code
	 * try (PandAAPI api = PandAAPI.newLogin("ecsid", "password", System.out::println)) {
	 *     ...
	 * }
	 * }
	 * </pre>
	 * @param ecsId ECS-ID
	 * @param password パスワード
	 * @param log ログの出力先
	 * @return PandAAPI オブジェクト
	 */
	public static PandAAPI newLogin(String ecsId, String password, LogTarget log) throws PandAAPIException {
		PandAAPI api = new PandAAPI(log);
		api.login(ecsId, password);
		return api;
	}
	
	/**
	 * PandAAPI.newLogin(ecsId, password, null) と同じ効果が得られる.
	 * @param ecsId ECS-ID
	 * @param password パスワード
	 * @return PandAAPI オブジェクト
	 */
	public static PandAAPI newLogin(String ecsId, String password) throws PandAAPIException {
		return newLogin(ecsId, password, null);
	}
	
	/**
	 * PandAAPI オブジェクトの終了処理を行う.
	 * try-with-resource 文を利用すれば終了処理を自動的に行うこともできる.
	 * @see PandAAPI#newLogin(String, String, LogTarget)
	 * @see PandAAPI#newLogin(String, String)
	 */
	@Override
	public void close() throws PandAAPIException {
		if (loginTried) {
			logout();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
