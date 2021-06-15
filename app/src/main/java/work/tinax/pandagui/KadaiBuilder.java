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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

public class KadaiBuilder {
	private String idField;
	private String lectureField;
	private String titleField;
	private LocalDateTime dueField;
	private String descriptionField;
	private boolean isQuiz = false;
	
	public KadaiBuilder id(String id) {
		idField = id;
		return this;
	}
	
	public KadaiBuilder lecture(String lecture) {
		lectureField = lecture;
		return this;
	}
	
	public KadaiBuilder title(String title) {
		titleField = title;
		return this;
	}
	
	public KadaiBuilder dueString(String due) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("%y/%M/%d %h:%m");
		dueField = formatter.parse(due, temporalAccessor -> LocalDateTime.from(temporalAccessor));
		return this;
	}
	
	public KadaiBuilder due(LocalDateTime due) {
		dueField = due;
		return this;
	}
	
	public KadaiBuilder description(String description) {
		descriptionField = description;
		return this;
	}

	public KadaiBuilder asQuiz() {
		isQuiz = true;
		return this;
	}
	
	/**
	 * Kadai オブジェクトを作成する. id(), lecture(), title(), due(), description() が少なくとも一度以上事前に呼ばれている必要がある.
	 * @return Kadai オブジェクトを作成する.
	 * @throws IllegalStateException id(), lecture(), title(), due(), description() のうち一つ以上, 呼ばれていないメソッドがある場合
	 */
	public Kadai build() {
		if (idField == null || lectureField == null || titleField == null || dueField == null || descriptionField == null) {
			throw new IllegalStateException("kadai is not complete");
		}
		return new Kadai(idField, titleField, lectureField, descriptionField, dueField, isQuiz);
	}
}
