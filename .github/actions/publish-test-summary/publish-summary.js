module.exports = async ({ core }) => {
  const fs = require('fs');
  const path = require('path');

  function findXmlFiles(dir) {
    let results = [];
    if (!fs.existsSync(dir)) return results;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        results = results.concat(findXmlFiles(fullPath));
      } else if (entry.isFile() && entry.name.endsWith('.xml')) {
        results.push(fullPath);
      }
    }
    return results;
  }

  const searchDirs = (process.env.RESULTS_DIRS || '').split(',').map(s => s.trim()).filter(Boolean);

  let xmlFiles = [];
  for (const d of searchDirs) {
    xmlFiles = xmlFiles.concat(findXmlFiles(d));
  }
  xmlFiles = [...new Set(xmlFiles)];

  const title = process.env.SUMMARY_TITLE || 'Test Summary';

  if (xmlFiles.length === 0) {
    await core.summary
      .addHeading(`🧪 ${title}`, 3)
      .addRaw('No JUnit XML report files found.')
      .write();
    return;
  }

  let totalTests = 0, totalFailures = 0, totalErrors = 0, totalSkipped = 0, totalTime = 0.0;
  const cases = [];

  for (const file of xmlFiles) {
    try {
      const content = fs.readFileSync(file, 'utf8');

      const suiteMatches = content.matchAll(/<testsuite\b([^>]*)>/g);
      for (const m of suiteMatches) {
        const attrs = m[1];
        const getAttr = (name) => {
          const match = attrs.match(new RegExp(`${name}="([^"]*)"`));
          return match ? match[1] : null;
        };
        totalTests += parseInt(getAttr('tests') || '0', 10);
        totalFailures += parseInt(getAttr('failures') || '0', 10);
        totalErrors += parseInt(getAttr('errors') || '0', 10);
        totalSkipped += parseInt(getAttr('skipped') || getAttr('disabled') || '0', 10);
        totalTime += parseFloat(getAttr('time') || '0');
      }

      const caseMatches = content.matchAll(/<testcase\b([^/>]*)(?:\/>|>([\s\S]*?)<\/testcase>)/g);
      for (const m of caseMatches) {
        const attrs = m[1] || '';
        const body = m[2] || '';
        const getAttr = (name) => {
          const match = attrs.match(new RegExp(`${name}="([^"]*)"`));
          return match ? match[1] : '';
        };
        const classname = getAttr('classname');
        const name = getAttr('name');
        const timeVal = getAttr('time') || '0';

        let status = '✅ Passed';
        let details = '';

        if (body.includes('<failure')) {
          status = '❌ Failed';
          const failMatch = body.match(/<failure[^>]*>([\s\S]*?)<\/failure>/);
          details = failMatch ? failMatch[1] : '';
        } else if (body.includes('<error')) {
          status = '💥 Error';
          const errMatch = body.match(/<error[^>]*>([\s\S]*?)<\/error>/);
          details = errMatch ? errMatch[1] : '';
        } else if (body.includes('<skipped')) {
          status = '⚠️ Skipped';
        }

        if (classname && name) {
          cases.push({ classname, name, timeVal, status, details });
        }
      }
    } catch (e) {
      console.error(`Error parsing ${file}:`, e);
    }
  }

  const passed = Math.max(0, totalTests - totalFailures - totalErrors - totalSkipped);
  const statusIcon = (totalFailures === 0 && totalErrors === 0 && totalTests > 0) ? '✅' : '❌';

  let summary = core.summary
    .addHeading(`${statusIcon} ${title}`, 3)
    .addTable([
      [
        { data: 'Total', header: true },
        { data: 'Passed', header: true },
        { data: 'Failed', header: true },
        { data: 'Errors', header: true },
        { data: 'Skipped', header: true },
        { data: 'Duration', header: true }
      ],
      [
        String(totalTests),
        String(passed),
        String(totalFailures),
        String(totalErrors),
        String(totalSkipped),
        `${totalTime.toFixed(2)}s`
      ]
    ]);

  if (totalFailures > 0 || totalErrors > 0) {
    summary = summary.addRaw('\n<details><summary><b>Failed Test Details</b></summary>\n\n');
    for (const c of cases) {
      if (c.status.includes('Failed') || c.status.includes('Error')) {
        const shortClass = c.classname.split('.').pop();
        summary = summary.addRaw(`#### ${c.status}: \`${shortClass}.${c.name}\` (${c.timeVal}s)\n\n`);
        if (c.details) {
          summary = summary.addRaw(`\`\`\`\n${c.details.trim().slice(0, 1000)}\n\`\`\`\n\n`);
        }
      }
    }
    summary = summary.addRaw('</details>\n\n');
  }

  if (cases.length > 0) {
    summary = summary
      .addRaw('\n<details><summary><b>All Test Cases</b></summary>\n\n')
      .addTable([
        [
          { data: 'Status', header: true },
          { data: 'Class', header: true },
          { data: 'Test Case', header: true },
          { data: 'Time', header: true }
        ],
        ...cases.map(c => [
          c.status,
          `\`${c.classname.split('.').pop()}\``,
          `\`${c.name}\``,
          `${c.timeVal}s`
        ])
      ])
      .addRaw('\n</details>\n\n');
  }

  await summary.write();
};
