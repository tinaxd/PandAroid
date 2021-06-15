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

import java.io.Serializable;
import java.time.LocalDateTime;

public class Kadai implements Serializable {
	private final String id;
	private final String lectureName;
	private final String kadaiName;
	private final String description;
	private final LocalDateTime due;
	private final boolean isQuiz;

	public static final String QUIZ_ID_PREFIX = "q";

	private static final long serialVersionUID = 1L;
	
	public Kadai(String id, String kadaiName, String lectureName, String description, LocalDateTime due) {
		this(id, kadaiName, lectureName, description, due, false);
	}

	public Kadai(String id, String kadaiName, String lectureName, String description, LocalDateTime due, boolean isQuiz) {
		this.id = id;
		this.kadaiName = kadaiName;
		this.lectureName = lectureName;
		this.description = description;
		this.due = due;
		this.isQuiz = isQuiz;
	}

	public String getId() {
		return id;
	}

	public String getLectureName() {
		return lectureName;
	}

	public String getKadaiName() {
		return kadaiName;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getDue() {
		return due;
	}

	public boolean isQuiz() {
		return isQuiz;
	}
	
	/**
	 * o と this が等価であるか確認する.
	 * @return o が Kadai のインスタンスで, かつ, o.getId() == this.getId() であれば true. それ以外の場合 false.
	 * @param o 比較対象のオブジェクト
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Kadai) {
			Kadai k = (Kadai) o;
			return id.equals(k.getId());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("Kadai(id=%s, name=%s, due=%s, description=%s, isQuiz=%s)", id, kadaiName, due.toString(), description, isQuiz);
	}
}
