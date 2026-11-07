# Third-party source notices

MMCE Complement retains its existing project name, author information, namespace,
and MIT license. The files below contain adaptations of separately licensed code.

## AppliedCompatibility

- Project: <https://github.com/FormlessDragon/AppliedCompatibility>
- Version: 1.0.1, commit `52470006c38a4e0e7278d62c1f4664b6e6b1f224`
- License: GNU Lesser General Public License, version 3 or later
- Original files:
  - `common/container/mmce/ContainerMEPatternProvider.java`
  - `client/gui/mmce/GuiMEPatternProvider.java`
- Adapted files in this project:
  - `src/main/java/net/edwin/mmcecomplement/compat/ae2s/AE2SPatternProviderContainer.java`
  - `src/main/java/net/edwin/mmcecomplement/compat/ae2s/client/AE2SPatternProviderGui.java`

The adaptations provide Complement's 144 pattern slots, three independent fluid
slots, and native AE2S container actions and synchronization. These adapted files,
including their modifications, are distributed under LGPL-3.0-or-later. Their
corresponding source is included in this project's sources jar.

The LGPL and GPL texts are included in the installation jar under
`META-INF/licenses/`. AppliedCompatibility, MMCE, and AE2S remain separate runtime
dependencies; their jars are not embedded or modified by this project.

## MMCE Complement PR #1

- Author: Butterfly-Kiyo
- Pull request: https://github.com/Greediest-Studio/MMCE-Complement/pull/1
- Commits: `9c6a5ac611b65dafa7dc7caa9dc883be1a11315e`,
  `d6687a11b67956a4242bc45cb5116bdb9b883aa9`
- Same upstream project MIT license, original author attribution retained.

The inventory listener adapter incorporates the custom primitive/boxed `accept`
callbacks and proxy identity behavior, retaining the older `onChange` callback.
The two server-safe item-name translations are included, together with the
contributor's side-safety regression test. This records integration of the changes;
the original pull request remains under its author's control.
