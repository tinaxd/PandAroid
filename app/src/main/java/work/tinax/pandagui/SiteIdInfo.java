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

public class SiteIdInfo {
	private final String siteId;
	private final String lectureName;
	
	SiteIdInfo(String siteId, String lectureName) {
		this.siteId = siteId;
		this.lectureName = lectureName;
	}
	
	public String getSiteId() {
		return siteId;
	}
	
	public String getLectureName() {
		return lectureName;
	}
}
