%define __os_install_post \
%{nil}
Name: is24-complex-test
Version: 1.1.0
Release: 2
Summary: Test Rpm for new Repo Server
Group: is24-tf
License: LGPL
Vendor: IS24 Cld TF
URL: http://www.immobilienscout24.de/
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-root
Requires: a_require > 1,b_require >= 2,c_require < 3, d_require <= 4,e_require = 5
Requires(pre): pre_require
Provides: a_provides,b_provides,c_provides
Obsoletes: a_obsoletes,b_obsoletes,c_obsoletes

%define originalSourceName archiveWithFiles-1.0.0

SOURCE0: %{originalSourceName}.tar.gz

%description
This RPM is used to test our RPM header parser.

%prep
%setup -n %{originalSourceName}

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/empty_dir
cp -r * %{buildroot}

find %{buildroot} -printf "/%%P\n" > files.list

%clean
rm -rf %{buildroot}

%files -f files.list
%attr(775, webadmin,admins) /a_file.txt

%ghost %attr(775, webadmin,admins) /z_ghost.txt


%changelog
* Tue Feb 02 2013 - second
- added second part I
- added second part II

* Mon Feb 01 2013 - first
- added first

